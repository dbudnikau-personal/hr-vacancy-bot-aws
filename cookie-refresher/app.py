import logging
import boto3
from playwright.sync_api import sync_playwright, TimeoutError as PlaywrightTimeoutError

logger = logging.getLogger()
logger.setLevel(logging.INFO)

ssm = boto3.client('ssm')

SSM_DATADOME   = '/hrbot/wellfound/datadome'
SSM_CF_CLEARANCE = '/hrbot/wellfound/cf-clearance'

TARGET_URL = 'https://wellfound.com/role/r/software-engineer'


def handler(event, context):
    logger.info('Starting Wellfound cookie refresh')

    with sync_playwright() as p:
        browser = p.chromium.launch(
            headless=True,
            args=[
                '--no-sandbox',
                '--disable-setuid-sandbox',
                '--disable-dev-shm-usage',
                '--disable-gpu',
                '--single-process',
                '--no-zygote',
            ],
        )
        ctx = browser.new_context(
            user_agent='Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) '
                       'AppleWebKit/537.36 (KHTML, like Gecko) '
                       'Chrome/124.0.0.0 Safari/537.36',
            viewport={'width': 1280, 'height': 800},
            locale='en-US',
        )

        page = ctx.new_page()

        try:
            logger.info('Navigating to %s', TARGET_URL)
            page.goto(TARGET_URL, wait_until='networkidle', timeout=60_000)
            # Give DataDome/Cloudflare JS extra time to settle
            page.wait_for_timeout(4_000)
        except PlaywrightTimeoutError:
            logger.warning('Page load timed out — cookies may still be available')

        cookies = ctx.cookies()
        cookie_map = {c['name']: c['value'] for c in cookies}

        datadome    = cookie_map.get('datadome', '')
        cf_clearance = cookie_map.get('cf_clearance', '')

        logger.info('Cookies extracted — datadome=%s cf_clearance=%s',
                    bool(datadome), bool(cf_clearance))

        browser.close()

    _put_ssm(SSM_DATADOME,    datadome)
    _put_ssm(SSM_CF_CLEARANCE, cf_clearance)

    result = {'datadome': bool(datadome), 'cf_clearance': bool(cf_clearance)}
    logger.info('Cookie refresh complete: %s', result)
    return result


def _put_ssm(name: str, value: str):
    if not value:
        logger.warning('Skipping empty value for SSM param %s', name)
        return
    ssm.put_parameter(Name=name, Value=value, Type='SecureString', Overwrite=True)
    logger.info('Saved %s to SSM', name)
