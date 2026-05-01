import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
import { getAuth, signInAnonymously } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";
import {
  collection,
  doc,
  getDocs,
  getFirestore,
  limit,
  onSnapshot,
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
const commonNoteInput = document.getElementById("commonNote");
const mobileCartBar = document.getElementById("mobileCartBar");
const mobileCartSummary = document.getElementById("mobileCartSummary");
const mobileCartBackdrop = document.getElementById("mobileCartBackdrop");
const closeCartBtn = document.getElementById("closeCartBtn");

const params = new URLSearchParams(window.location.search);
const tableCode = (params.get("table") || "").trim().toUpperCase();

const FALLBACK_CATEGORY = {
  categoryId: "all",
  name: "Tất cả món",
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
let tableUnsubscribe = null;
let categoryUnsubscribe = null;
let productUnsubscribe = null;

boot();

async function boot() {
  if (!tableCode) {
    tableDisplay.textContent = "Không hợp lệ";
    authStatus.textContent = "Lỗi";
    submitOrderBtn.disabled = true;
    renderCategories();
    renderMenu();
    renderCart();
    showError("QR này chưa có mã bàn. Vui lòng quét lại mã trên bàn.");
    return;
  }

  tableDisplay.textContent = tableCode;
  renderCategories();
  renderMenu();
  renderCart();

  try {
    await signInAnonymously(auth);
    authStatus.textContent = "Đã kết nối";
    currentTable = await fetchTableByCode(tableCode);
    if (!currentTable) {
      showError("Không tìm thấy bàn tương ứng. Vui lòng báo nhân viên hỗ trợ.");
      submitOrderBtn.disabled = true;
      return;
    }

    tableDisplay.textContent = formatTableDisplay(currentTable);
    await refreshCatalogOnce();
    startTableRealtime(tableCode);
    startCatalogRealtime();
    submitOrderBtn.disabled = false;
  } catch (error) {
    authStatus.textContent = "Lỗi kết nối";
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

function startTableRealtime(code) {
  if (tableUnsubscribe) {
    tableUnsubscribe();
  }

  const tableQuery = query(
    collection(db, "tables"),
    where("code", "==", code),
    limit(1)
  );

  tableUnsubscribe = onSnapshot(tableQuery, (snapshot) => {
    if (snapshot.empty) {
      currentTable = null;
      submitOrderBtn.disabled = true;
      showError("Bàn này không còn khả dụng. Vui lòng báo nhân viên hỗ trợ.");
      return;
    }

    const nextTable = snapshot.docs[0].data();
    const activeValue = nextTable.is_active ?? nextTable.active;
    if (activeValue === false) {
      currentTable = null;
      submitOrderBtn.disabled = true;
      showError("Bàn này đang tạm ngưng nhận đơn.");
      return;
    }

    currentTable = nextTable;
    tableDisplay.textContent = formatTableDisplay(currentTable);
    submitOrderBtn.disabled = false;
  }, (error) => {
    showError(normalizeError(error));
  });
}

function startCatalogRefresh() {
  return;
  if (catalogRefreshHandle) {
    window.clearInterval(catalogRefreshHandle);
  }
  catalogRefreshHandle = window.setInterval(() => {
    refreshCatalogFromServer().catch(() => {
      // Giữ UI hiện tại, lần sau thử lại.
    });
  }, 5000);
}

async function refreshCatalogOnce() {
  const [categorySnapshot, productSnapshot] = await Promise.all([
    getDocs(collection(db, "categories")),
    getDocs(collection(db, "products"))
  ]);

  rawCategories = categorySnapshot.docs.map(mapCategoryDocument);
  rawProducts = productSnapshot.docs.map(mapProductDocument);
  rebuildCatalogState();
}

function startCatalogRealtime() {
  stopCatalogRealtime();

  categoryUnsubscribe = onSnapshot(collection(db, "categories"), (snapshot) => {
    rawCategories = snapshot.docs.map(mapCategoryDocument);
    rebuildCatalogState();
  }, (error) => {
    showError(normalizeError(error));
  });

  productUnsubscribe = onSnapshot(collection(db, "products"), (snapshot) => {
    rawProducts = snapshot.docs.map(mapProductDocument);
    rebuildCatalogState();
  }, (error) => {
    showError(normalizeError(error));
  });
}

function stopCatalogRealtime() {
  if (categoryUnsubscribe) {
    categoryUnsubscribe();
    categoryUnsubscribe = null;
  }
  if (productUnsubscribe) {
    productUnsubscribe();
    productUnsubscribe = null;
  }
}

window.addEventListener("beforeunload", () => {
  if (tableUnsubscribe) {
    tableUnsubscribe();
  }
  stopCatalogRealtime();
});

function mapCategoryDocument(snapshot) {
  const activeValue = snapshot.get("is_active");
  const legacyActiveValue = snapshot.get("active");
  return {
    categoryId: snapshot.get("category_id") || snapshot.get("categoryId") || snapshot.id,
    name: snapshot.get("name") || "Chưa đặt tên",
    imageUrl: readAssetUrl(snapshot, ["image_url", "imageUrl", "image", "photo_url", "photoUrl", "thumbnail_url", "thumbnailUrl", "download_url", "downloadUrl"]),
    active: (activeValue !== undefined ? activeValue : legacyActiveValue) !== false
  };
}

function mapProductDocument(snapshot) {
  const activeValue = snapshot.get("is_active");
  const legacyActiveValue = snapshot.get("active");
  const basePrice = snapshot.get("base_price") ?? snapshot.get("basePrice") ?? snapshot.get("unit_price") ?? snapshot.get("unitPrice");
  const legacyPrice = snapshot.get("price");
  return {
    productId: snapshot.get("product_id") || snapshot.get("productId") || snapshot.id,
    name: snapshot.get("name") || snapshot.get("product_name") || snapshot.get("productName") || "Chưa đặt tên",
    price: Number(basePrice ?? legacyPrice ?? 0),
    categoryId: snapshot.get("category_id") || snapshot.get("categoryId") || "",
    imageUrl: readAssetUrl(snapshot, ["image_url", "imageUrl", "image", "photo_url", "photoUrl", "thumbnail_url", "thumbnailUrl", "download_url", "downloadUrl"]),
    description: snapshot.get("description") || "Món được chuẩn bị ngay sau khi quầy nhận đơn.",
    availableSizes: parseSizes(snapshot.get("available_sizes") ?? snapshot.get("availableSizes")),
    active: (activeValue !== undefined ? activeValue : legacyActiveValue) !== false
  };
}

function parseSizes(rawValue) {
  if (!Array.isArray(rawValue)) {
    return [];
  }
  return rawValue
    .map((item) => String(item || "").trim().toUpperCase())
    .filter((item, index, values) => ["S", "M", "L"].includes(item) && values.indexOf(item) === index);
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
        category: category?.name || "Khác",
        categoryImageUrl: normalizeAssetUrl(category?.imageUrl),
        accentLabel: category?.name || "Món mới"
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
    showInfo(`Đã tải ${fetchedProducts.length} món và ${fetchedCategories.length} danh mục.`);
  } else {
    showInfo("Hiện chưa có món hợp lệ để hiển thị.");
  }
}

function renderCategories() {
  categoryList.innerHTML = "";
  categories.forEach((category) => {
    const visual = getCategoryVisual(category);
    const thumbStyle = `background:${visual.image};`;
    const card = document.createElement("button");
    card.type = "button";
    card.className = `category-card${category.categoryId === activeCategory ? " active" : ""}`;
    card.innerHTML = `
      <div class="category-thumb" style="${thumbStyle}">
        ${renderImageTag(category.imageUrl, category.name, "category-img")}
      </div>
      <div class="category-label">${escapeHtml(category.name)}</div>
    `;
    bindImageFallback(card);
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
    categories.find((item) => item.categoryId === activeCategory)?.name || "Tất cả món";
  menuHeading.textContent = activeCategoryName;
  menuGrid.innerHTML = "";
  menuEmpty.classList.toggle("hidden", filteredItems.length > 0);

  filteredItems.forEach((item) => {
    const visual = getProductVisual(item);
    const productVisualUrl = normalizeAssetUrl(item.imageUrl) || normalizeAssetUrl(item.categoryImageUrl);
    const cardBackground = `background:${visual.image};`;
    const plateBackground = `background:${visual.plate};`;

    const card = document.createElement("article");
    card.className = "product-card";
    card.innerHTML = `
      <div class="product-visual" style="${cardBackground}">
        <span class="product-badge">${escapeHtml(item.accentLabel)}</span>
        <div class="product-image-shell" style="${plateBackground}">
          ${renderImageTag(productVisualUrl, item.name, "product-img")}
        </div>
      </div>

      <div class="product-body">
        <div>
          <h3>${escapeHtml(item.name)}</h3>
          <p>${escapeHtml(item.description || "Món được chuẩn bị ngay sau khi quầy nhận đơn.")}</p>
        </div>
        <div class="product-spacer"></div>
        <div class="product-price">${formatMoney(item.price)}</div>

        <div class="product-footer">
          <div class="qty-box">
            <span>SL</span>
            <input class="qty-input form-control form-control-sm" type="number" min="1" value="1" aria-label="Số lượng ${escapeHtml(item.name)}">
          </div>
          <div class="qty-box">
            <span>Size</span>
            <select class="size-select form-select form-select-sm" aria-label="Chọn size ${escapeHtml(item.name)}">
              <option value="">Chọn</option>
              ${(item.availableSizes || []).map((size) => `<option value="${escapeAttribute(size)}">${escapeHtml(size)}</option>`).join("")}
            </select>
          </div>
          <button class="add-btn btn btn-warning shadow-sm" type="button" aria-label="Thêm ${escapeHtml(item.name)}">+</button>
        </div>
      </div>
    `;

    const qtyInput = card.querySelector(".qty-input");
    const sizeSelect = card.querySelector(".size-select");
    const addButton = card.querySelector(".add-btn");
    bindImageFallback(card);
    addButton.addEventListener("click", () => {
      const qty = Math.max(1, parseInt(qtyInput.value || "1", 10));
      const selectedSize = sizeSelect ? (sizeSelect.value || "").trim().toUpperCase() : "";
      if (sizeSelect && !selectedSize) {
        showError(`Vui lòng chọn size cho ${item.name}.`);
        return;
      }
      addToCart(item, qty, selectedSize);
    });

    menuGrid.appendChild(card);
  });
}

function addToCart(item, qty, size = "") {
  const existing = cart.find((entry) => entry.productId === item.productId && (entry.size || "") === size);
  if (existing) {
    existing.qty += qty;
  } else {
    cart.push({
      productId: item.productId,
      name: item.name,
      unitPrice: item.price,
      qty,
      size: size || null,
      variantName: size ? `Size ${size}` : null,
      note: "",
      categoryId: item.categoryId,
      category: item.category,
      imageUrl: normalizeAssetUrl(item.imageUrl) || normalizeAssetUrl(item.categoryImageUrl)
    });
  }
  renderCart();
  showInfo(`Đã thêm ${qty} ${item.name}.`);
  if (window.innerWidth <= 1040) {
    openMobileCart();
  }
}

function renderCart() {
  cartList.innerHTML = "";
  emptyState.style.display = cart.length === 0 ? "block" : "none";
  const totalCount = cart.reduce((sum, item) => sum + item.qty, 0);
  cartCount.textContent = `${totalCount} món`;

  let subtotal = 0;
  cart.forEach((item, index) => {
    subtotal += item.unitPrice * item.qty;
    const visual = getProductVisual(item);
    const thumbStyle = item.imageUrl
      ? `background-image:url('${escapeAttribute(item.imageUrl)}');`
      : `background:${visual.plate};`;
    const node = document.createElement("article");
    node.className = "cart-item";
    node.innerHTML = `
      <div class="cart-item-top">
        <div class="cart-thumb" style="${thumbStyle}"></div>
        <div>
          <div class="cart-title">${escapeHtml(item.name)}</div>
          <div class="cart-sub">${formatMoney(item.unitPrice)} x ${item.qty}${item.size ? ` • Size ${escapeHtml(item.size)}` : ""}</div>
        </div>
        <div class="cart-price">${formatMoney(item.unitPrice * item.qty)}</div>
      </div>

      <div class="cart-controls">
        <div class="cart-controls-left">
          <span>SL</span>
          <input type="number" min="1" value="${item.qty}" aria-label="Cập nhật số lượng ${escapeHtml(item.name)}">
        </div>
        <button class="inline-btn" type="button">Xóa</button>
      </div>

      <div class="cart-note">
        <input type="text" value="${escapeAttribute(item.note)}" placeholder="Ghi chú riêng cho món này">
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
  mobileCartSummary.textContent = `${totalCount} món - ${formatMoney(subtotal)}`;
  mobileCartBar.classList.toggle("hidden", totalCount === 0);
}

submitOrderBtn.addEventListener("click", async () => {
  if (!currentTable) {
    showError("Chưa xác định được bàn. Vui lòng quét lại QR.");
    return;
  }
  if (cart.length === 0) {
    showError("Hãy chọn ít nhất 1 món trước khi gửi đơn.");
    return;
  }

  submitOrderBtn.disabled = true;
  authStatus.textContent = "Đang gửi đơn";

  try {
    const commonNote = commonNoteInput.value.trim();
    const incomingItems = cart.map((item) => ({
      productId: item.productId,
      productName: item.name,
      variantName: item.variantName || null,
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
      const addedQty = incomingItems.reduce((sum, item) => sum + item.qty, 0);

      await updateDoc(doc(db, "orders", existingOrder.orderId), {
        status: "created",
        subtotal,
        total: subtotal,
        updatedAt: serverTimestamp(),
        lastCustomerAction: "item_added",
        lastCustomerItemAddedAt: Date.now(),
        lastCustomerItemAddedQty: addedQty,
        lastCustomerAddedItems: incomingItems,
        needsStaffAttention: true,
        note: commonNote || existingOrder.note || null,
        customerName: existingOrder.customerName || null,
        customerPhone: existingOrder.customerPhone || null,
        items: mergedItems,
        productId: primaryItem?.productId || null,
        productName: primaryItem?.productName || null,
        unitPrice: primaryItem?.unitPrice || 0,
        qty: primaryItem?.qty || 0,
        variantName: null,
        imageUrl: primaryItem?.imageUrl || null
      });
      const notificationId = `table_order_item_added_${existingOrder.orderId}_${Date.now()}`;
      await tryWriteStaffNotification(notificationId, {
        eventKey: notificationId,
        type: "table_order_item_added",
        title: "Bàn vừa thêm món",
        body: `${currentTable.name} vừa thêm ${addedQty} món vào đơn đang mở.`,
        orderId: existingOrder.orderId,
        orderCode: existingOrder.orderCode || null,
        orderChannel: "customer_qr",
        tableId: currentTable.tableId,
        tableName: currentTable.name,
        status: "created"
      });
      showSuccess(`Đã cộng thêm món vào đơn đang mở của ${currentTable.name}.`);
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
        customerName: null,
        customerPhone: null,
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
      await tryWriteStaffNotification(`new_table_order_${orderId}`, {
        eventKey: `new_table_order_${orderId}`,
        type: "new_table_order",
        title: "Đơn tại bàn mới",
        body: `${currentTable.name} vừa tạo đơn mới (${orderCode}).`,
        orderId,
        orderCode,
        orderChannel: "customer_qr",
        tableId: currentTable.tableId,
        tableName: currentTable.name,
        status: "created"
      });
      showSuccess(`Đã gửi đơn cho ${currentTable.name}. Nhân viên sẽ nhận được ngay.`);
    }

    cart = [];
    renderCart();
    commonNoteInput.value = "";
    authStatus.textContent = "Đã gửi đơn";
    closeMobileCart();
  } catch (error) {
    authStatus.textContent = "Lỗi gửi đơn";
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
  mobileCartBar.setAttribute("aria-expanded", "true");
}

function closeMobileCart() {
  document.body.classList.remove("cart-open");
  mobileCartBar.setAttribute("aria-expanded", "false");
}

window.addEventListener("resize", () => {
  if (window.innerWidth > 1040) {
    closeMobileCart();
  }
});

function formatMoney(value) {
  return `${new Intl.NumberFormat("en-US").format(Number(value) || 0)}đ`;
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

async function writeStaffNotification(notificationId, payload) {
  await setDoc(doc(db, "staff_notifications", notificationId), {
    id: notificationId,
    targetRole: "staff",
    createdAt: Date.now(),
    updatedAt: serverTimestamp(),
    pushDispatchedAt: null,
    ...payload
  });
}

async function tryWriteStaffNotification(notificationId, payload) {
  try {
    await writeStaffNotification(notificationId, payload);
  } catch (error) {
    console.warn("Skip staff_notifications write:", normalizeError(error));
  }
}

function mergeItems(existingItems, incomingItems) {
  const merged = existingItems.map((item) => ({ ...item }));

  incomingItems.forEach((incoming) => {
    const existing = merged.find((item) =>
      item.productId === incoming.productId &&
      (item.variantName || "") === (incoming.variantName || "") &&
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

function readAssetUrl(snapshot, fieldNames) {
  for (const fieldName of fieldNames) {
    const normalized = normalizeAssetUrl(snapshot.get(fieldName));
    if (normalized) {
      return normalized;
    }
  }
  return "";
}

function renderImageTag(url, alt, className) {
  const normalized = normalizeAssetUrl(url);
  if (!normalized) {
    return "";
  }
  return `<img class="${className}" src="${escapeAttribute(normalized)}" alt="${escapeAttribute(alt || "CafePlus")}" loading="lazy" decoding="async" data-fallback="true">`;
}

function bindImageFallback(root) {
  root.querySelectorAll("img[data-fallback]").forEach((image) => {
    image.addEventListener("error", () => {
      image.remove();
    }, { once: true });
  });
}

function normalizeAssetUrl(value) {
  if (value === null || value === undefined) {
    return "";
  }
  const normalized = String(value).trim();
  if (!normalized || normalized.toLowerCase() === "null" || normalized.toLowerCase() === "undefined") {
    return "";
  }
  const lower = normalized.toLowerCase();
  if (
    lower.startsWith("content://") ||
    lower.startsWith("file://") ||
    lower.startsWith("android.resource://")
  ) {
    return "";
  }
  if (lower.startsWith("gs://")) {
    return toFirebaseStorageDownloadUrl(normalized);
  }
  return normalized;
}

function toFirebaseStorageDownloadUrl(value) {
  const withoutScheme = value.slice(5);
  const slashIndex = withoutScheme.indexOf("/");
  if (slashIndex <= 0 || slashIndex >= withoutScheme.length - 1) {
    return "";
  }
  const bucket = withoutScheme.slice(0, slashIndex);
  const path = withoutScheme.slice(slashIndex + 1);
  return `https://firebasestorage.googleapis.com/v0/b/${bucket}/o/${encodeURIComponent(path)}?alt=media`;
}

function formatTableDisplay(table) {
  const name = table.name || "";
  const code = table.code || "";
  if (name && code) {
    return `${name} | ${code}`;
  }
  return name || code || tableCode;
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
    return "Có lỗi xảy ra khi kết nối hệ thống.";
  }
  if (typeof error === "string") {
    return error;
  }
  return error.message || "Có lỗi xảy ra khi kết nối hệ thống.";
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
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function escapeAttribute(value) {
  return escapeHtml(value).replaceAll("`", "&#96;");
}
