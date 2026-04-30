FROM public.ecr.aws/lambda/python:3.12

# Chromium system dependencies
RUN dnf install -y \
    atk \
    cups-libs \
    libdrm \
    libXcomposite \
    libXdamage \
    libXrandr \
    libgbm \
    libxkbcommon \
    pango \
    alsa-lib \
    nss \
    && dnf clean all

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt && \
    playwright install chromium

COPY app.py .

CMD ["app.handler"]
