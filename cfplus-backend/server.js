import cors from "cors";
import "dotenv/config";
import express from "express";

const app = express();
const port = process.env.PORT || 3000;
const allowedOrigin = process.env.ALLOWED_ORIGIN || "*";
const aiProvider = (process.env.AI_PROVIDER || "openrouter").toLowerCase();

const SYSTEM_PROMPT =
  "Ban la tro ly AI cua CFPLUS, mot ung dung dat ca phe. Tra loi bang tieng Viet co dau, tu nhien, ngan gon nhung huu ich. Hay tu van nhu nhan vien quan ca phe cao cap: hoi khau vi khi can, goi y theo do ngot, do dam, sua, da, nong/lanh, caffeine va ngan sach. Chi ho tro khach ve menu, goi y mon, gio mo cua, khuyen mai, dia chi, dat hang va thanh toan. Khong bia gia, ma giam gia, ton kho hay chinh sach neu backend chua cung cap du lieu. Neu khong chac du lieu thuc te, noi ro va huong dan khach kiem tra trong app hoac lien he nhan vien.";

app.use(cors({ origin: allowedOrigin }));
app.use(express.json({ limit: "64kb" }));

app.get("/health", (req, res) => {
  res.json({
    ok: true,
    service: "cfplus-backend",
    aiProvider
  });
});

app.post("/api/chat", async (req, res) => {
  try {
    const message = String(req.body?.message || "").trim();
    if (!message) {
      return res.status(400).json({ error: "Missing message." });
    }
    if (message.length > 1000) {
      return res.status(400).json({ error: "Message is too long." });
    }

    const history = normalizeHistory(req.body?.history);
    const menuContext = normalizeMenuContext(req.body?.menu_context);
    const reply = await askAi(message, history, menuContext);

    return res.json({ reply });
  } catch (error) {
    return res.status(500).json({
      error: error?.message || "Backend server error."
    });
  }
});

async function askAi(message, history = [], menuContext = []) {
  if (aiProvider === "g4f") {
    return askG4f(message, history, menuContext);
  }
  if (aiProvider === "openai") {
    return askOpenAi(message, history, menuContext);
  }
  return askOpenRouter(message, history, menuContext);
}

async function askOpenRouter(message, history = [], menuContext = []) {
  const apiKey = process.env.OPENROUTER_API_KEY;
  const model = process.env.OPENROUTER_MODEL || "openai/gpt-4o-mini";
  const referer = process.env.OPENROUTER_SITE_URL || "https://cfplus.local";
  const title = process.env.OPENROUTER_APP_NAME || "CFPLUS Coffee";
  if (!apiKey) {
    throw new Error("OPENROUTER_API_KEY is not configured on backend.");
  }

  const response = await fetch("https://openrouter.ai/api/v1/chat/completions", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${apiKey}`,
      "Content-Type": "application/json",
      "HTTP-Referer": referer,
      "X-Title": title
    },
    body: JSON.stringify({
      model,
      messages: buildChatMessages(message, history, menuContext),
      temperature: 0.4,
      max_tokens: 350
    })
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data?.error?.message || "OpenRouter request failed.");
  }
  return extractChatCompletionsReply(data);
}

async function askOpenAi(message, history = [], menuContext = []) {
  const apiKey = process.env.OPENAI_API_KEY;
  const model = process.env.OPENAI_MODEL || "gpt-4.1-mini";
  if (!apiKey) {
    throw new Error("OPENAI_API_KEY is not configured on backend.");
  }

  const response = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${apiKey}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      model,
      input: buildChatMessages(message, history, menuContext),
      temperature: 0.4,
      max_output_tokens: 350
    })
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data?.error?.message || "OpenAI request failed.");
  }
  return extractOpenAiResponseReply(data);
}

async function askG4f(message, history = [], menuContext = []) {
  const baseUrl = (process.env.G4F_BASE_URL || "http://127.0.0.1:1337").replace(/\/$/, "");
  const apiKey = process.env.G4F_API_KEY || "";
  const model = process.env.G4F_MODEL || "openai/gpt-oss-120b";

  const headers = {
    "Content-Type": "application/json"
  };
  if (apiKey) {
    headers.Authorization = `Bearer ${apiKey}`;
  }

  const response = await fetch(`${baseUrl}/v1/chat/completions`, {
    method: "POST",
    headers,
    body: JSON.stringify({
      model,
      messages: buildChatMessages(message, history, menuContext),
      temperature: 0.4,
      max_tokens: 350
    })
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data?.error?.message || data?.detail || "G4F request failed.");
  }
  return extractChatCompletionsReply(data);
}

function normalizeHistory(history) {
  if (!Array.isArray(history)) {
    return [];
  }
  return history
    .slice(-10)
    .map((item) => ({
      role: item?.role === "assistant" ? "assistant" : "user",
      content: String(item?.content || "").trim()
    }))
    .filter((item) => item.content && item.content.length <= 1000);
}

function normalizeMenuContext(menuContext) {
  if (!Array.isArray(menuContext)) {
    return [];
  }
  return menuContext
    .slice(0, 30)
    .map((item) => ({
      name: String(item?.name || "").trim(),
      price: Number(item?.price || 0),
      category: String(item?.category || "").trim()
    }))
    .filter((item) => item.name && item.price >= 0);
}

function buildMenuContextPrompt(menuContext = []) {
  if (!menuContext.length) {
    return "";
  }
  const lines = menuContext
    .map((item) => `- ${item.name} | ${item.price.toLocaleString("vi-VN")}d | ${item.category || "Khac"}`)
    .join("\n");
  return [
    "Du lieu menu hien tai cua CFPLUS tu Firebase. Khi khach hoi mon ngon/menu/goi y, hay uu tien goi y dung ten mon trong danh sach nay de app co the hien card san pham.",
    "Neu khach hoi khau vi, chon 1-3 mon phu hop va noi ngan gon ly do.",
    lines
  ].join("\n");
}

function buildChatMessages(message, history = [], menuContext = []) {
  const messages = [{ role: "system", content: SYSTEM_PROMPT }];
  const menuPrompt = buildMenuContextPrompt(menuContext);
  if (menuPrompt) {
    messages.push({ role: "system", content: menuPrompt });
  }
  messages.push(...normalizeHistory(history));

  const last = messages[messages.length - 1];
  if (!last || last.role !== "user" || last.content !== message) {
    messages.push({ role: "user", content: message });
  }

  return messages;
}

function extractOpenAiResponseReply(data) {
  if (typeof data?.output_text === "string" && data.output_text.trim()) {
    return data.output_text.trim();
  }

  for (const item of data?.output || []) {
    for (const content of item?.content || []) {
      if (typeof content?.text === "string" && content.text.trim()) {
        return content.text.trim();
      }
    }
  }

  return "Minh chua co cau tra loi ro rang. Ban hoi lai ngan hon giup minh nhe.";
}

function extractChatCompletionsReply(data) {
  const content = data?.choices?.[0]?.message?.content;
  if (typeof content === "string" && content.trim()) {
    return content.trim();
  }
  return "Minh chua co cau tra loi ro rang. Ban hoi lai ngan hon giup minh nhe.";
}

app.listen(port, () => {
  console.log(`CFPLUS backend listening on port ${port} with provider ${aiProvider}`);
});
