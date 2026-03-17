try:
    import pytesseract
    from PIL import Image
except ImportError:
    pytesseract = None
    Image = None


class ReceiptScanner:
    def scan(self, image_path):
        if not image_path:
            return {"text": "", "amount": None}

        if not pytesseract or not Image:
            print("⚠ OCR not available, skipping receipt scan")
            return {"text": "", "amount": None}

        try:
            img = Image.open(image_path)
            text = pytesseract.image_to_string(img)
            return {"text": text, "amount": None}
        except Exception as e:
            print(f"⚠ Receipt scan failed: {e}")
            return {"text": "", "amount": None}
