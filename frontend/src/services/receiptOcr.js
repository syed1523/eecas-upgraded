import Groq from 'groq-sdk';

const GROQ_API_KEY = import.meta.env.VITE_GROQ_API_KEY;
console.log('Groq API Key loaded:', GROQ_API_KEY ? 'YES' : 'NO - CHECK .env FILE');

const fileToBase64 = (file) => {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result.split(',')[1]);
        reader.onerror = reject;
        reader.readAsDataURL(file);
    });
};

const withRetry = async (fn, onProgress, retries = 3, delay = 5000) => {
    for (let i = 0; i < retries; i++) {
        try {
            return await fn();
        } catch (err) {
            const is429 = err?.message?.includes('429') ||
                err?.message?.includes('quota') ||
                err?.message?.includes('Too Many Requests');
            if (is429 && i < retries - 1) {
                if (onProgress) onProgress(-1);
                await new Promise(res => setTimeout(res, delay * (i + 1)));
                continue;
            }
            throw err;
        }
    }
};

export const extractReceiptData = async (imageFile, onProgress) => {
    try {
        onProgress(10);
        const client = new Groq({
            apiKey: GROQ_API_KEY,
            dangerouslyAllowBrowser: true
        });

        onProgress(30);
        const base64Data = await fileToBase64(imageFile);
        const mimeType = imageFile.type || 'image/jpeg';

        onProgress(50);

        const response = await withRetry(() => client.chat.completions.create({
            model: 'meta-llama/llama-4-scout-17b-16e-instruct',
            messages: [
                {
                    role: 'user',
                    content: [
                        {
                            type: 'image_url',
                            image_url: {
                                url: `data:${mimeType};base64,${base64Data}`,
                            },
                        },
                        {
                            type: 'text',
                            text: `You are analyzing an Indian receipt image.
              Return ONLY a valid JSON object, no markdown, no explanation:
              {
                "vendor": "business name from receipt",
                "amount": "final total as plain number only e.g. 1450.00, INR only",
                "date": "date in DD-MM-YYYY format",
                "category": "one of: Meals, Travel, Office Supplies, Software, Training, Other",
                "confidence": {
                  "vendor": "High/Medium/Low",
                  "amount": "High/Medium/Low",
                  "date": "High/Medium/Low",
                  "category": "High/Medium/Low"
                },
                "currencyDetected": "INR or FOREIGN",
                "notes": "invoice number or GST number if visible"
              }
              Rules:
              - amount must be FINAL payable total, not subtotal
              - If foreign currency found set currencyDetected FOREIGN and amount to ""
              - Empty string "" for any field not found
              - For Indian receipts: look for ₹ symbol, Rs., INR, or just number after TOTAL/GRAND TOTAL`
                        }
                    ],
                }
            ],
            max_tokens: 500,
        }), onProgress);

        onProgress(85);

        const text = response.choices[0]?.message?.content?.trim() || '';
        const cleaned = text.replace(/```json|```/g, '').trim();
        const parsed = JSON.parse(cleaned);

        onProgress(100);

        return {
            vendor: parsed.vendor || '',
            amount: parsed.amount || '',
            date: parsed.date || '',
            category: parsed.category || '',
            confidence: parsed.confidence || {
                vendor: 'Low', amount: 'Low', date: 'Low', category: 'Low'
            },
            currencyDetected: parsed.currencyDetected || 'INR',
            notes: parsed.notes || '',
            rawText: `Extracted by Groq AI\nNotes: ${parsed.notes || 'None'}`,
        };

    } catch (error) {
        console.error('Groq OCR failed:', error);
        return {
            vendor: '', amount: '', date: '', category: '',
            confidence: { vendor: 'Low', amount: 'Low', date: 'Low', category: 'Low' },
            currencyDetected: 'INR', notes: '',
            rawText: 'Receipt scanning temporarily unavailable. Please fill in the fields manually.',
            error: true,
        };
    }
};
