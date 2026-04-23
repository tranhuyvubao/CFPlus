# CFPLUS Backend

Backend nay cung cap endpoint chat cho Android app. Mac dinh backend goi OpenRouter, nhung cung co the cau hinh de goi OpenAI truc tiep hoac local `gpt4free-main` API.

## Chay local

```bash
cd cfplus-backend
npm install
copy .env.example .env
npm run dev
```

Sua `.env`:

```env
AI_PROVIDER=openrouter
OPENROUTER_API_KEY=sk-or-your-openrouter-api-key
OPENROUTER_MODEL=openai/gpt-4o-mini
OPENROUTER_SITE_URL=https://cfplus.local
OPENROUTER_APP_NAME=CFPLUS Coffee
PORT=3000
ALLOWED_ORIGIN=*
```

## Dung OpenRouter

Tao key tren OpenRouter, roi dan vao file `.env`:

```env
AI_PROVIDER=openrouter
OPENROUTER_API_KEY=sk-or-your-openrouter-api-key
OPENROUTER_MODEL=openai/gpt-4o-mini
```

Neu model tren khong dung duoc voi tai khoan cua ban, doi `OPENROUTER_MODEL` sang model khac trong dashboard OpenRouter.

## Dung OpenAI truc tiep

```env
AI_PROVIDER=openai
OPENAI_API_KEY=sk-your-openai-api-key
OPENAI_MODEL=gpt-4.1-mini
```

## Dung gpt4free-main local

`gpt4free-main` co OpenAI-compatible API tai `/v1/chat/completions`. Repo nay tu ghi ro muc dich giao duc va khong dam bao on dinh, nen chi nen dung demo/test.

Chay g4f API:

```bash
cd ../gpt4free-main
pip install -r requirements-min.txt
pip install fastapi uvicorn
python -m g4f api --port 1337 --no-gui --debug
```

Sua `.env` cua `cfplus-backend`:

```env
AI_PROVIDER=g4f
G4F_BASE_URL=http://127.0.0.1:1337
G4F_MODEL=openai/gpt-oss-120b
PORT=3000
```

Sau do chay backend CFPLUS:

```bash
cd ../cfplus-backend
npm run dev
```

Neu provider/model cua g4f bi loi, doi `G4F_MODEL` hoac chay GUI/API g4f de xem model/provider dang hoat dong.

Test:

```bash
curl -X POST http://localhost:3000/api/chat ^
  -H "Content-Type: application/json" ^
  -d "{\"message\":\"Goi y mon ngon cho toi\"}"
```

## Noi Android app voi backend

Sau khi deploy backend, sua:

`app/src/main/java/com/example/do_an_hk1_androidstudio/config/ChatBackendConfig.java`

```java
public static final String CHAT_API_URL = "https://your-backend-domain.com/api/chat";
```

Khong dua OpenRouter/OpenAI API key vao Android app. Key chi nam trong `.env` cua backend.
