import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
import { getAuth, signInAnonymously } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";
import {
  collection,
  doc,
  getDocs,
  getDocsFromServer,
  getFirestore,
  limit,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  where
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js";

const firebaseConfig = {
  apiKey: "AIzaSyAkS5x8aqlgEi0EAfCJQxG2iQISA1IxRso",
  authDomain: "cafeplus-1fd32.firebaseapp.com",
  projectId: "cafeplus-1fd32",
  storageBucket: "cafeplus-1fd32.firebasestorage.app",
  messagingSenderId: "105381932857",
  appId: "1:105381932857:web:demo-cafeplus-order"
};

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app);

const tableDisplay = document.getElementById("tableDisplay");
const authStatus = document.getElementById("authStatus");
const categoryList = document.getElementById("categoryList");
const menuHeading = document.getElementById("menuHeading");
const searchInput = document.getElementById("searchInput");
const menuGrid = document.getElementById("menuGrid");
const menuEmpty = document.getElementById("menuEmpty");
const emptyState = document.getElementById("emptyState");
const cartList = document.getElementById("cartList");
const cartCount = document.getElementById("cartCount");
const subtotalValue = document.getElementById("subtotalValue");
const grandTotalValue = document.getElementById("grandTotalValue");
const submitOrderBtn = document.getElementById("submitOrderBtn");
const messageBox = document.getElementById("messageBox");
const customerNameInput = document.getElementById("customerName");
const customerPhoneInput = document.getElementById("customerPhone");
const commonNoteInput = document.getElementById("commonNote");
const mobileCartBar = document.getElementById("mobileCartBar");
const mobileCartSummary = document.getElementById("mobileCartSummary");
const mobileCartBackdrop = document.getElementById("mobileCartBackdrop");
const closeCartBtn = document.getElementById("closeCartBtn");

const params = new URLSearchParams(window.location.search);
const tableCode = (params.get("table") || "").trim().toUpperCase();

const FALLBACK_CATEGORY = {
  categoryId: "all",
  name: "Tat ca mon",
  imageUrl: "",
  active: true
};

let categories = [FALLBACK_CATEGORY];
let menuItems = [];
let activeCategory = FALLBACK_CATEGORY.categoryId;
let currentTable = null;
let cart = [];
let rawCategories = [];
let rawProducts = [];
let catalogRefreshHandle = null;

boot();

async function boot() {
  if (!tableCode) {
    tableDisplay.textContent = "Khong hop le";
    authStatus.textContent = "Loi";
    submitOrderBtn.disabled = true;
    renderCategories();
    renderMenu();
    renderCart();
    showError("QR nay chua co ma ban. Vui long quet lai ma tren ban.");
    return;
  }

  tableDisplay.textContent = tableCode;
  renderCategories();
  renderMenu();
  renderCart();

  try {
    await signInAnonymously(auth);
    authStatus.textContent = "Da ket noi";
    currentTable = await fetchTableByCode(tableCode);
    if (!currentTable) {
      showError("Khong tim thay ban tuong ung. Vui long bao nhan vien ho tro.");
      submitOrderBtn.disabled = true;
      return;
    }

    await refreshCatalogFromServer();
    startCatalogRefresh();

    tableDisplay.textContent = `${currentTable.name} • ${currentTable.code}`;
    submitOrderBtn.disabled = false;
  } catch (error) {
    authStatus.textContent = "Loi ket noi";
    submitOrderBtn.disabled = true;
    showError(normalizeError(error));
  }
}

async function fetchTableByCode(code) {
  const tableQuery = query(
    collection(db, "tables"),
    where("code", "==", code),
    where("active", "==", true),
    limit(1)
  );
  const snapshot = await getDocs(tableQuery);
  if (snapshot.empty) {
    return null;
  }
  return snapshot.docs[0].data();
}

async function refreshCatalogFromServer() {
  const [categorySnapshot, productSnapshot] = await Promise.all([
    getDocsFromServer(collection(db, "categories")),
    getDocsFromServer(collection(db, "products"))
  ]);

  rawCategories = categorySnapshot.docs.map(mapCategoryDocument);
  rawProducts = productSnapshot.docs.map(mapProductDocument);
  rebuildCatalogState();
}

function startCatalogRefresh() {
  if (catalogRefreshHandle) {
    window.clearInterval(catalogRefreshHandle);
  }
  catalogRefreshHandle = window.setInterval(() => {
    refreshCatalogFromServer().catch(() => {
      // Keep current UI while retrying.
    });
  }, 5000);
}

function mapCategoryDocument(snapshot) {
  const activeValue = snapshot.get("is_active");
  const legacyActiveValue = snapshot.get("active");
  return {
    categoryId: snapshot.get("category_id") || snapshot.get("categoryId") || snapshot.id,
    name: snapshot.get("name") || "Chua dat ten",
    imageUrl: snapshot.get("image_url") || snapshot.get("imageUrl") || "",
    active: (activeValue !== undefined ? activeValue : legacyActiveValue) !== false
  };
}

function mapProductDocument(snapshot) {
  const activeValue = snapshot.get("is_active");
  const legacyActiveValue = snapshot.get("active");
  const basePrice = snapshot.get("base_price");
  const legacyPrice = snapshot.get("price");
  return {
    productId: snapshot.get("product_id") || snapshot.get("productId") || snapshot.id,
    name: snapshot.get("name") || snapshot.get("product_name") || snapshot.get("productName") || "Chua dat ten",
    price: Number(basePrice ?? legacyPrice ?? 0),
    categoryId: snapshot.get("category_id") || snapshot.get("categoryId") || "",
    imageUrl: snapshot.get("image_url") || snapshot.get("imageUrl") || "",
    description: snapshot.get("description") || "Mon duoc chuan bi ngay sau khi quay nhan don.",
    active: (activeValue !== undefined ? activeValue : legacyActiveValue) !== false
  };
}

function rebuildCatalogState() {
  const fetchedCategories = rawCategories
    .filter((category) => category.active)
    .sort((a, b) => a.name.localeCompare(b.name, "vi"));

  const categoryMap = new Map(
    fetchedCategories.map((category) => [category.categoryId, category])
  );

  const fetchedProducts = rawProducts
    .map((item) => {
      const category = categoryMap.get(item.categoryId);
      return {
        ...item,
        category: category?.name || "Khac",
        categoryImageUrl: category?.imageUrl || "",
        accentLabel: category?.name || "Mon moi"
      };
    })
    .filter((item) => item.active && item.price > 0)
    .sort((a, b) => a.name.localeCompare(b.name, "vi"));

  categories = [FALLBACK_CATEGORY, ...fetchedCategories];
  menuItems = fetchedProducts;
  if (!categories.some((item) => item.categoryId === activeCategory)) {
    activeCategory = FALLBACK_CATEGORY.categoryId;
  }
  renderCategories();
  renderMenu();
  if (fetchedProducts.length > 0) {
    showInfo(`Da tai ${fetchedProducts.length} mon va ${fetchedCategories.length} danh muc.`);
  } else {
    showInfo(`Hien co ${fetchedCategories.length} danh muc, nhung chua co mon hop le de hien thi.`);
  }
}

function renderCategories() {
  categoryList.innerHTML = "";
  categories.forEach((category) => {
    const visual = getCategoryVisual(category);
    const thumbStyle = category.imageUrl
      ? `background-image:url('${category.imageUrl}');`
      : `background:${visual.image};`;
    const card = document.createElement("button");
    card.type = "button";
    card.className = `category-card${category.categoryId === activeCategory ? " active" : ""}`;
    card.innerHTML = `
      <div class="category-thumb" style="${thumbStyle}"></div>
      <div class="category-label">${escapeHtml(category.name)}</div>
    `;
    card.addEventListener("click", () => {
      activeCategory = category.categoryId;
      renderCategories();
      renderMenu();
    });
    categoryList.appendChild(card);
  });
}

function getFilteredMenuItems() {
  const keyword = (searchInput.value || "").trim().toLowerCase();
  return menuItems.filter((item) => {
    const matchCategory = activeCategory === FALLBACK_CATEGORY.categoryId || item.categoryId === activeCategory;
    const matchKeyword =
      !keyword ||
      item.name.toLowerCase().includes(keyword) ||
      item.category.toLowerCase().includes(keyword) ||
      (item.description || "").toLowerCase().includes(keyword);
    return matchCategory && matchKeyword;
  });
}

function renderMenu() {
  const filteredItems = getFilteredMenuItems();
  const activeCategoryName =
    categories.find((item) => item.categoryId === activeCategory)?.name || "Tat ca mon";
  menuHeading.textContent = activeCategoryName;
  menuGrid.innerHTML = "";
  menuEmpty.classList.toggle("hidden", filteredItems.length > 0);

  filteredItems.forEach((item) => {
    const visual = getProductVisual(item);
    const cardBackground = item.imageUrl
      ? `background-image:url('${item.imageUrl}');`
      : `background:${visual.image};`;
    const plateBackground = item.imageUrl
      ? `background-image:url('${item.imageUrl}');`
      : item.categoryImageUrl
        ? `background-image:url('${item.categoryImageUrl}');`
        : `background:${visual.plate};`;

    const card = document.createElement("article");
    card.className = "product-card";
    card.innerHTML = `
      <div class="product-visual" style="${cardBackground}">
        <span class="product-badge">${escapeHtml(item.accentLabel)}</span>
        <div class="product-image" style="${plateBackground}"></div>
      </div>

      <div class="product-body">
        <div>
          <h3>${escapeHtml(item.name)}</h3>
          <p>${escapeHtml(item.description || "Mon duoc chuan bi ngay sau khi quay nhan don.")}</p>
        </div>
        <div class="product-spacer"></div>
        <div class="product-price">${formatMoney(item.price)}</div>

        <div class="product-footer">
          <div class="qty-box">
            <span>SL</span>
            <input class="qty-input" type="number" min="1" value="1" aria-label="So luong ${escapeHtml(item.name)}">
          </div>
          <button class="add-btn" type="button" aria-label="Them ${escapeHtml(item.name)}">+</button>
        </div>
      </div>
    `;

    const qtyInput = card.querySelector(".qty-input");
    const addButton = card.querySelector(".add-btn");
    addButton.addEventListener("click", () => {
      const qty = Math.max(1, parseInt(qtyInput.value || "1", 10));
      addToCart(item, qty);
    });

    menuGrid.appendChild(card);
  });
}

function addToCart(item, qty) {
  const existing = cart.find((entry) => entry.productId === item.productId);
  if (existing) {
    existing.qty += qty;
  } else {
    cart.push({
      productId: item.productId,
      name: item.name,
      unitPrice: item.price,
      qty,
      note: "",
      categoryId: item.categoryId,
      category: item.category,
      imageUrl: item.imageUrl || item.categoryImageUrl || ""
    });
  }
  renderCart();
  showInfo(`Da them ${qty} ${item.name}.`);
  if (window.innerWidth <= 1040) {
    openMobileCart();
  }
}

function renderCart() {
  cartList.innerHTML = "";
  emptyState.style.display = cart.length === 0 ? "block" : "none";
  const totalCount = cart.reduce((sum, item) => sum + item.qty, 0);
  cartCount.textContent = `${totalCount} mon`;

  let subtotal = 0;
  cart.forEach((item, index) => {
    subtotal += item.unitPrice * item.qty;
    const visual = getProductVisual(item);
    const thumbStyle = item.imageUrl
      ? `background-image:url('${item.imageUrl}');`
      : `background:${visual.plate};`;
    const node = document.createElement("article");
    node.className = "cart-item";
    node.innerHTML = `
      <div class="cart-item-top">
        <div class="cart-thumb" style="${thumbStyle}"></div>
        <div>
          <div class="cart-title">${escapeHtml(item.name)}</div>
          <div class="cart-sub">${formatMoney(item.unitPrice)} x ${item.qty}</div>
        </div>
        <div class="cart-price">${formatMoney(item.unitPrice * item.qty)}</div>
      </div>

      <div class="cart-controls">
        <div class="cart-controls-left">
          <span>SL</span>
          <input type="number" min="1" value="${item.qty}" aria-label="Cap nhat so luong ${escapeHtml(item.name)}">
        </div>
        <button class="inline-btn" type="button">Xoa</button>
      </div>

      <div class="cart-note">
        <input type="text" value="${escapeHtml(item.note)}" placeholder="Ghi chu rieng cho mon nay">
      </div>
    `;

    const qtyInput = node.querySelector('input[type="number"]');
    const deleteButton = node.querySelector(".inline-btn");
    const noteInput = node.querySelector(".cart-note input");

    qtyInput.addEventListener("change", () => {
      item.qty = Math.max(1, parseInt(qtyInput.value || "1", 10));
      renderCart();
    });
    deleteButton.addEventListener("click", () => {
      cart.splice(index, 1);
      renderCart();
    });
    noteInput.addEventListener("input", () => {
      item.note = noteInput.value.trim();
    });

    cartList.appendChild(node);
  });

  subtotalValue.textContent = formatMoney(subtotal);
  grandTotalValue.textContent = formatMoney(subtotal);
  mobileCartSummary.textContent = `${totalCount} mon • ${formatMoney(subtotal)}`;
  mobileCartBar.classList.toggle("hidden", totalCount === 0);
}

submitOrderBtn.addEventListener("click", async () => {
  if (!currentTable) {
    showError("Chua xac dinh duoc ban. Vui long quet lai QR.");
    return;
  }
  if (cart.length === 0) {
    showError("Hay chon it nhat 1 mon truoc khi gui don.");
    return;
  }

  submitOrderBtn.disabled = true;
  authStatus.textContent = "Dang gui don";

  try {
    const customerName = customerNameInput.value.trim();
    const customerPhone = customerPhoneInput.value.trim();
    const commonNote = commonNoteInput.value.trim();
    const incomingItems = cart.map((item) => ({
      productId: item.productId,
      productName: item.name,
      qty: item.qty,
      unitPrice: item.unitPrice,
      lineTotal: item.unitPrice * item.qty,
      note: item.note || null,
      imageUrl: item.imageUrl || null
    }));
    const existingOrder = await findOpenOrderForTable(currentTable.tableId);

    if (existingOrder) {
      const mergedItems = mergeItems(existingOrder.items || [], incomingItems);
      const subtotal = mergedItems.reduce((sum, item) => sum + (item.lineTotal || 0), 0);
      const primaryItem = mergedItems[0];

      await updateDoc(doc(db, "orders", existingOrder.orderId), {
        status: "created",
        subtotal,
        total: subtotal,
        updatedAt: serverTimestamp(),
        note: commonNote || existingOrder.note || null,
        customerName: customerName || existingOrder.customerName || null,
        customerPhone: customerPhone || existingOrder.customerPhone || null,
        items: mergedItems,
        productId: primaryItem?.productId || null,
        productName: primaryItem?.productName || null,
        unitPrice: primaryItem?.unitPrice || 0,
        qty: primaryItem?.qty || 0,
        variantName: null,
        imageUrl: primaryItem?.imageUrl || null
      });
      showSuccess(`Da cong them mon vao don dang mo cua ${currentTable.name}.`);
    } else {
      const subtotal = incomingItems.reduce((sum, item) => sum + item.lineTotal, 0);
      const orderId = `web_order_${crypto.randomUUID().replace(/-/g, "")}`;
      const orderCode = buildOrderCode(new Date());
      const primaryItem = incomingItems[0];

      await setDoc(doc(db, "orders", orderId), {
        orderId,
        orderCode,
        orderType: "dine_in",
        orderChannel: "customer_qr",
        tableId: currentTable.tableId,
        tableName: currentTable.name,
        tableCode: currentTable.code,
        customerId: auth.currentUser?.uid || null,
        customerName: customerName || null,
        customerPhone: customerPhone || null,
        staffId: null,
        status: "created",
        subtotal,
        discountAmount: 0,
        total: subtotal,
        createdAt: Date.now(),
        updatedAt: serverTimestamp(),
        note: commonNote || null,
        items: incomingItems,
        productId: primaryItem.productId,
        productName: primaryItem.productName,
        unitPrice: primaryItem.unitPrice,
        qty: primaryItem.qty,
        variantName: null,
        imageUrl: primaryItem.imageUrl || null
      });
      showSuccess(`Da gui don cho ${currentTable.name}. Nhan vien se nhan duoc ngay.`);
    }

    cart = [];
    renderCart();
    customerNameInput.value = "";
    customerPhoneInput.value = "";
    commonNoteInput.value = "";
    authStatus.textContent = "Da gui don";
    closeMobileCart();
  } catch (error) {
    authStatus.textContent = "Loi gui don";
    showError(normalizeError(error));
  } finally {
    submitOrderBtn.disabled = false;
  }
});

searchInput.addEventListener("input", renderMenu);
mobileCartBar.addEventListener("click", openMobileCart);
mobileCartBackdrop.addEventListener("click", closeMobileCart);
closeCartBtn.addEventListener("click", closeMobileCart);

function openMobileCart() {
  if (window.innerWidth > 1040) {
    return;
  }
  document.body.classList.add("cart-open");
}

function closeMobileCart() {
  document.body.classList.remove("cart-open");
}

window.addEventListener("resize", () => {
  if (window.innerWidth > 1040) {
    closeMobileCart();
  }
});

function formatMoney(value) {
  return `${new Intl.NumberFormat("vi-VN").format(value)} VND`;
}

function buildOrderCode(date = new Date()) {
  const pad = (value) => String(value).padStart(2, "0");
  return [
    pad(date.getHours()),
    pad(date.getMinutes()),
    pad(date.getSeconds()),
    pad(date.getDate()),
    pad(date.getMonth() + 1),
    date.getFullYear()
  ].join("");
}

async function findOpenOrderForTable(tableId) {
  const openOrderQuery = query(
    collection(db, "orders"),
    where("tableId", "==", tableId),
    where("status", "in", ["created", "confirmed"]),
    limit(1)
  );
  const snapshot = await getDocs(openOrderQuery);
  if (snapshot.empty) {
    return null;
  }
  return snapshot.docs[0].data();
}

function mergeItems(existingItems, incomingItems) {
  const merged = existingItems.map((item) => ({ ...item }));

  incomingItems.forEach((incoming) => {
    const existing = merged.find((item) =>
      item.productId === incoming.productId &&
      (item.note || "") === (incoming.note || "")
    );
    if (existing) {
      existing.qty += incoming.qty;
      existing.lineTotal = existing.unitPrice * existing.qty;
      return;
    }
    merged.push({ ...incoming });
  });

  return merged;
}

function getCategoryVisual(category) {
  const palette = paletteFromText(category.name || category.categoryId || "menu");
  return {
    image: `linear-gradient(145deg, ${palette[0]}, ${palette[1]})`,
    plate: `radial-gradient(circle at 30% 30%, ${palette[2]}, ${palette[1]} 56%, ${palette[0]} 100%)`
  };
}

function getProductVisual(item) {
  const palette = paletteFromText(`${item.categoryId || ""}${item.name || ""}`);
  return {
    image: `linear-gradient(180deg, ${palette[2]} 0 24%, ${palette[0]} 100%)`,
    plate: `radial-gradient(circle at 35% 30%, ${palette[2]}, ${palette[1]} 58%, ${palette[0]} 100%)`
  };
}

function paletteFromText(value) {
  const palettes = [
    ["#4c2a17", "#9a6124", "#f3d279"],
    ["#2f4d28", "#7baa48", "#dff0ae"],
    ["#5f2f24", "#bf6a33", "#ffd9a2"],
    ["#3a2f58", "#8360b6", "#d8c6ff"],
    ["#27505e", "#50a4b8", "#c5f4ff"]
  ];
  let hash = 0;
  for (let index = 0; index < value.length; index += 1) {
    hash = ((hash << 5) - hash) + value.charCodeAt(index);
    hash |= 0;
  }
  return palettes[Math.abs(hash) % palettes.length];
}

function normalizeError(error) {
  if (!error) {
    return "Co loi xay ra khi ket noi he thong.";
  }
  if (typeof error === "string") {
    return error;
  }
  return error.message || "Co loi xay ra khi ket noi he thong.";
}

function showSuccess(message) {
  messageBox.className = "message success";
  messageBox.textContent = message;
}

function showError(message) {
  messageBox.className = "message error";
  messageBox.textContent = message;
}

function showInfo(message) {
  messageBox.className = "message";
  messageBox.textContent = message;
}

function escapeHtml(value) {
  return (value || "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;");
}
