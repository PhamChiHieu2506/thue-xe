"use strict";
(() => {
  const cfg = window.VELODRIVE_CONFIG,
    $ = (s, r = document) => r.querySelector(s),
    $$ = (s, r = document) => [...r.querySelectorAll(s)];
  const state = {
    provinces: [],
    catalog: [],
    inventoryError: null,
    inventoryLoading: false,
    searchVersion: 0,
    token: null,
    user: null,
    cars: [],
    trips: [],
    filters: {},
    photos: [],
    editing: null,
    quote: null,
    selected: null,
  };
  const esc = (v) =>
    String(v ?? "").replace(
      /[&<>"']/g,
      (c) =>
        ({
          "&": "&amp;",
          "<": "&lt;",
          ">": "&gt;",
          '"': "&quot;",
          "'": "&#39;",
        })[c],
    );
  const money = (n) =>
    new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
      maximumFractionDigits: 0,
    }).format(n || 0);
  const date = (v) =>
    v
      ? new Date(v).toLocaleString("vi-VN", {
          dateStyle: "short",
          timeStyle: "short",
        })
      : "—";
  const localDate = (d) =>
    new Date(d.getTime() - d.getTimezoneOffset() * 60000)
      .toISOString()
      .slice(0, 16);
  const tomorrow = new Date(Date.now() + 86400000);
  tomorrow.setHours(9, 0, 0, 0);
  state.filters.startsAt = localDate(tomorrow);
  state.filters.endsAt = localDate(new Date(tomorrow.getTime() + 2 * 86400000));
  const labels = {
    DRAFT: "Chưa đăng",
    PENDING_REVIEW: "Cần cập nhật",
    PUBLISHED: "Đang đăng",
    ARCHIVED: "Đã lưu trữ",
    PENDING: "Chờ chủ xe duyệt",
    APPROVED: "Chờ thanh toán giữ chỗ",
    CONFIRMED: "Đã xác nhận",
    IN_PROGRESS: "Đang thuê",
    COMPLETED: "Hoàn tất",
    CANCELLED: "Đã hủy",
    REJECTED: "Không chấp nhận",
    EXPIRED: "Hết hạn",
    VERIFIED: "Đã thực nhận",
    AVAILABLE: "Sẵn sàng",
    RENTED: "Tạm ngừng",
    MAINTENANCE: "Bảo dưỡng",
    INACTIVE: "Ngừng cho thuê",
    CUSTOMER: "Khách thuê",
    OWNER: "Chủ xe",
    ADMIN: "Quản trị viên",
  };
  const badge = (s) =>
    `<span class="badge ${["PENDING", "APPROVED", "DRAFT", "PENDING_REVIEW"].includes(s) ? "warn" : ["REJECTED", "CANCELLED", "EXPIRED"].includes(s) ? "red" : ""}">${esc(labels[s] || s)}</span>`;
  const button = (text, action, id = "", cls = "") =>
    `<button type="button" class="${cls}" data-action="${action}" data-id="${esc(id)}">${esc(text)}</button>`;
  const role = () => state.user?.role;
  const input = (label, name, value = "", type = "text", attrs = "") =>
    `<label>${label}<input name="${name}" type="${type}" value="${esc(value)}" ${attrs}></label>`;
  const select = (label, name, options, value = "", attrs = "") =>
    `<label>${label}<select name="${name}" ${attrs}>${options.map(([v, l]) => `<option value="${esc(v)}" ${String(v) === String(value) ? "selected" : ""}>${esc(l)}</option>`).join("")}</select></label>`;
  const empty = (title, text, action = "") =>
    `<div class="empty"><h3>${esc(title)}</h3><p>${esc(text)}</p>${action}</div>`;
  function toast(message) {
    const e = $("#toast");
    e.textContent = message;
    e.style.display = "block";
    clearTimeout(toast.timer);
    toast.timer = setTimeout(() => (e.style.display = "none"), 4500);
  }
  function showModal(title, html) {
    $("#modal-title").textContent = title;
    $("#modal-body").innerHTML = html;
    const d = $("#modal");
    if (!d.open) d.showModal();
  }
  function error(form, error) {
    let e = $(".error", form);
    if (!e) {
      e = document.createElement("p");
      e.className = "error";
      e.setAttribute("role", "alert");
      form.prepend(e);
    }
    e.textContent = error.message || "Không thể hoàn tất. Vui lòng thử lại.";
  }
  async function api(service, path, options = {}) {
    const origin =
      service === "auth"
        ? cfg.authOrigin
        : service === "rental"
          ? cfg.rentalOrigin
          : "";
    const headers = {
      ...(state.token ? { Authorization: `Bearer ${state.token}` } : {}),
      ...options.headers,
    };
    let body = options.body;
    if (body && !(body instanceof FormData)) {
      headers["Content-Type"] = "application/json";
      body = JSON.stringify(body);
    }
    const controller = new AbortController(),
      timer = setTimeout(() => controller.abort(), cfg.timeoutMs);
    let res;
    try {
      res = await fetch(origin + path, {
        ...options,
        body,
        headers,
        signal: controller.signal,
        cache: "no-store",
      });
    } catch (e) {
      throw new Error(
        e.name === "AbortError"
          ? "Dịch vụ phản hồi quá chậm. Kiểm tra lại trạng thái trước khi gửi lại."
          : "Không kết nối được dịch vụ. Kiểm tra Docker và thử lại.",
      );
    } finally {
      clearTimeout(timer);
    }
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      if (res.status === 401 && state.token) {
        logout(false);
        toast("Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại.");
      }
      throw new Error(
        data.message ||
          data.detail ||
          `Yêu cầu chưa thành công (${res.status}).`,
      );
    }
    return data;
  }
  function nav() {
    const items = [["cars", "Tìm xe"]];
    if (state.user) {
      if (role() === "OWNER")
        items.push(
          ["mycars", "Xe của tôi"],
          ["trips", "Yêu cầu & chuyến xe"],
          ["billing", "Nhận thanh toán"],
        );
      else if (role() === "ADMIN")
        items.push(
          ["vehicles", "Quản lý xe"],
          ["trips", "Đơn thuê"],
          ["accounts", "Tài khoản"],
        );
      else items.push(["trips", "Chuyến của tôi"]);
      items.push(["profile", "Hồ sơ"]);
    }
    $("#nav").innerHTML = items
      .map(
        ([id, name]) =>
          `<a class="${routeName() === id ? "active" : ""}" href="#${id}">${name}</a>`,
      )
      .join("");
    $("#session").innerHTML = state.user
      ? `<span>${esc(state.user.username)} · ${esc(labels[role()])}</span>${button("Đăng xuất", "logout")}`
      : `${button("Đăng nhập", "login")}${button("Đăng ký", "register", "", "primary")}`;
  }
  function routeName() {
    const name = location.hash.slice(1).split("?")[0] || "cars";
    return name === "reviews" ? "vehicles" : name;
  }
  function logout(render = true) {
    state.token = null;
    state.user = null;
    state.trips = [];
    state.quote = null;
    nav();
    if (render) {
      $("#modal").close();
      location.hash = "cars";
      route();
    }
  }
  function auth(mode = "login") {
    showModal(
      mode === "login" ? "Chào mừng trở lại" : "Tạo tài khoản VeloDrive",
      `<div class="authbox"><div class="auth-tabs">${button("Đăng nhập", "login", "", mode === "login" ? "primary" : "")}${button("Đăng ký", "register", "", mode === "register" ? "primary" : "")}</div><form id="auth-form" class="stack" data-mode="${mode}">${input("Tên đăng nhập", "username", "", "text", 'required minlength="3" maxlength="60" autocomplete="username" pattern="[A-Za-z0-9._-]+"')}${input("Mật khẩu", "password", "", "password", 'required minlength="6" maxlength="72" autocomplete="' + (mode === "login" ? "current-password" : "new-password") + '"')}${
        mode === "register"
          ? `${input("Họ tên", "displayName", "", "text", 'required minlength="2" maxlength="100" autocomplete="name"')}${input("Số điện thoại", "phone", "", "tel", 'required autocomplete="tel"')}${input("Email (không bắt buộc)", "email", "", "email", 'autocomplete="email"')}${select(
              "Bạn muốn sử dụng tài khoản để",
              "role",
              [
                ["CUSTOMER", "Thuê xe"],
                ["OWNER", "Đăng xe cho thuê"],
              ],
            )}<p class="hint">Mật khẩu từ 6 ký tự. Thông tin cá nhân được che khi hiển thị; điện thoại chỉ hiện 3 số cuối.</p>`
          : ""
      }<button class="primary" type="submit">${mode === "login" ? "Đăng nhập" : "Tạo tài khoản"}</button></form></div>`,
    );
  }
  const head = (title, text, actions = "") =>
    `<div class="page-head"><div><p class="eyebrow">VeloDrive / ${esc(labels[role()] || "Hành trình của bạn")}</p><h1>${title}</h1><p class="muted">${text}</p></div><div class="row">${actions}${button("Làm mới", "refresh")}</div></div>`;
  let directoryPromise;
  async function directories() {
    if (state.provinces.length && state.catalog.length) return;
    if (!directoryPromise)
      directoryPromise = Promise.all([
        api("car", "/api/locations/provinces"),
        api("car", "/api/car-catalog"),
      ])
        .then(([provinces, catalog]) => {
          state.provinces = provinces;
          state.catalog = catalog;
        })
        .catch((error) => {
          directoryPromise = null;
          throw error;
        });
    return directoryPromise;
  }
  const normalizeName = (value) =>
    String(value || "")
      .toLocaleLowerCase("vi-VN")
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .replace(/đ/g, "d")
      .replace(/^(?:tinh|thanh pho|tp\.?)\s*/, "")
      .replace(/[^a-z0-9]/g, "");
  function provinceValue(value) {
    const key = normalizeName(value);
    return (
      state.provinces.find((p) =>
        [p.code, p.name, p.label, ...p.aliases].some(
          (v) => normalizeName(v) === key,
        ),
      )?.name || ""
    );
  }
  function provinceSelect(label, name, value, required = false) {
    return select(
      label,
      name,
      [
        ["", required ? "Chọn tỉnh / thành phố" : "Toàn quốc"],
        ...state.provinces.map((p) => [p.name, p.label]),
      ],
      provinceValue(value),
      required ? "required" : "",
    );
  }
  function vehicleImage(url, name, extra = "") {
    return `<img src="${esc(url || "/images/fleet-garage.png")}" alt="${esc(name)}" loading="lazy" data-car-image ${extra}>`;
  }
  function modelForCar(car) {
    return (
      state.catalog.find((m) => m.id === car.modelId) ||
      state.catalog.find((m) =>
        normalizeName(`${car.brand} ${car.name}`).includes(
          normalizeName(m.name),
        ),
      )
    );
  }
  function matchesModel(car, model) {
    return modelForCar(car)?.id === model.id;
  }
  function modelPresentation(model) {
    const available = state.cars.filter((c) => matchesModel(c, model));
    const lowest = available.reduce(
      (best, car) =>
        !best || Number(car.pricePerDay) < Number(best.pricePerDay)
          ? car
          : best,
      null,
    );
    return lowest
      ? {
          pricePerDay: Number(lowest.pricePerDay),
          city: provinceValue(lowest.city) || lowest.city,
        }
      : {
          pricePerDay: Number(model.displayPricePerDay),
          city: model.displayCity,
        };
  }
  function selectedModels() {
    const f = state.filters;
    return state.catalog.filter((model) => {
      const view = modelPresentation(model);
      return (
        (!f.brand || model.brand === f.brand) &&
        (!f.seats || model.seatCount >= Number(f.seats)) &&
        (!f.model || model.id === f.model) &&
        (!f.city || provinceValue(view.city) === provinceValue(f.city)) &&
        (!f.maxPrice || view.pricePerDay <= Number(f.maxPrice))
      );
    });
  }
  function modelCard(model) {
    const view = modelPresentation(model);
    return `<article class="card model-card" data-model="${esc(model.id)}">
      <div class="car-image model-image">${vehicleImage(model.imageUrl, model.name)}<span class="badge">${esc(model.brand)}</span></div>
      <div class="card-body"><h3>${esc(model.name)}</h3><div class="car-meta"><span>${model.seatCount} chỗ</span><span>${esc(model.brand)}</span></div>
      <div class="card-bottom"><div class="model-rate"><span class="model-location" title="${esc(view.city)}">${esc(view.city)}</span><div class="price model-price">${view.pricePerDay > 0 ? money(view.pricePerDay) + " <small>/ngày</small>" : "Liên hệ"}</div></div>${button("Chọn dòng xe", "view-model", model.id, "primary")}</div></div></article>`;
  }
  function carCard(c, own = false) {
    return `<article class="card listing-card" data-car="${c.id}"><div class="car-image">${vehicleImage(c.imageUrl, c.name)}${own ? badge(c.listingStatus) : `<span class="badge">${esc(c.transmission === "MANUAL" ? "Số sàn" : "Số tự động")}</span>`}</div><div class="card-body"><h3>${esc(c.name)}</h3><p class="hint">${esc(provinceValue(c.city) || c.city || "Chưa có địa điểm")} · ${esc(c.district || "")}</p><div class="car-meta"><span>${c.seatCount} chỗ</span><span>${esc(c.brand)}</span><span>${esc(c.modelYear || "")}</span><span>${esc(c.fuelType === "ELECTRIC" ? "Xe điện" : c.fuelType === "HYBRID" ? "Hybrid" : "Động cơ đốt trong")}</span></div><div class="card-bottom"><div class="price">${money(c.pricePerDay)} <small>/ngày</small></div>${button(own ? "Chỉnh sửa" : "Xem xe", own ? "edit-car" : "view-car", c.id, own ? "" : "primary")}</div>${own ? `<div class="actions">${!c.published ? button("Đăng xe", "publish-car", c.id, "primary") : ""}${button("Lưu trữ", "archive-car", c.id, "danger")}</div><p class="hint">${esc(c.licensePlate)} · ${esc(labels[c.status])}</p>` : ""}</div></article>`;
  }
  function renderStorefrontResults() {
    const models = selectedModels();
    $("#car-count").textContent =
      `${models.length} dòng xe · ${state.inventoryLoading ? "đang kiểm tra lịch" : state.inventoryError ? "chưa kiểm tra lịch" : `${state.cars.length} xe có lịch trống`}`;
    $("#inventory-grid").innerHTML = state.cars.map((c) => carCard(c)).join("");
    $("#inventory-section").hidden = state.cars.length === 0;
    $("#inventory-note").innerHTML = state.inventoryLoading
      ? '<p class="inventory-hint">Đang kiểm tra lịch thuê…</p>'
      : state.inventoryError
        ? `<div class="notice inventory-notice">${esc(state.inventoryError)} ${button("Kiểm tra lại", "refresh")}</div>`
        : state.cars.length
          ? ""
          : `<p class="inventory-hint">Chọn dòng xe bên dưới để tìm xe tại ${esc(state.filters.city || "khu vực bạn muốn nhận xe")}.</p>`;
    $("#cars-grid").innerHTML =
      models.map(modelCard).join("") ||
      empty(
        "Chọn lại bộ lọc",
        "Thử hãng xe hoặc số chỗ khác.",
        button("Xóa bộ lọc", "reset-search"),
      );
  }
  async function cars() {
    await directories();
    const version = ++state.searchVersion;
    const f = state.filters;
    const brands = [
      ...new Set([
        ...state.catalog.map((m) => m.brand),
        ...(f.brand ? [f.brand] : []),
      ]),
    ];
    $("#app").innerHTML =
      `<section class="hero"><div><p class="eyebrow">Thuê xe tự lái · VeloDrive</p><h1>Chọn chiếc xe.<br>Mở lối hành trình.</h1></div><div class="hero-art" role="img" aria-label="Xe tự lái VeloDrive"></div></section>
      <form id="search-form"><div class="searchbar">${provinceSelect("Địa điểm nhận xe", "city", f.city)}${input("Nhận xe", "startsAt", f.startsAt, "datetime-local", "required")}${input("Trả xe", "endsAt", f.endsAt, "datetime-local", "required")}<button type="submit" class="primary">Tìm xe phù hợp</button></div>
      <div class="filters">${select("Hãng xe", "brand", [["", "Tất cả hãng"], ...brands.map((b) => [b, b])], f.brand)}${select(
        "Số chỗ",
        "seats",
        [
          ["", "Tất cả"],
          ["4", "Từ 4 chỗ"],
          ["5", "Từ 5 chỗ"],
          ["7", "Từ 7 chỗ"],
        ],
        f.seats,
      )}${input("Giá tối đa / ngày", "maxPrice", f.maxPrice || "", "number", 'min="0" placeholder="Không giới hạn"')}<input type="hidden" name="model" value="${esc(f.model || "")}">${f.model ? button("Bỏ chọn dòng xe", "clear-model") : ""}<span class="count" id="car-count"></span></div></form>
      <div id="inventory-note"></div><section id="inventory-section" hidden><div class="section-heading"><h2>Xe có lịch trống</h2></div><div id="inventory-grid" class="grid"></div></section>
      <section id="model-section"><div class="section-heading"><h2>Các dòng xe</h2></div><div id="cars-grid" class="grid"><p class="loading">Đang tải xe…</p></div></section>`;
    state.cars = [];
    state.inventoryError = null;
    state.inventoryLoading = true;
    renderStorefrontResults();
    const params = new URLSearchParams();
    for (const k of ["city", "brand", "seats", "maxPrice", "model"])
      if (f[k]) params.set(k, f[k]);
    const [inventory, calendar] = await Promise.allSettled([
      api("car", "/api/cars?" + params),
      api(
        "rental",
        "/api/availability?" +
          new URLSearchParams({
            startsAt: new Date(f.startsAt).toISOString(),
            endsAt: new Date(f.endsAt).toISOString(),
          }),
      ),
    ]);
    if (
      version !== state.searchVersion ||
      routeName() !== "cars" ||
      !$("#cars-grid")
    )
      return;
    state.inventoryLoading = false;
    if (inventory.status === "rejected" || calendar.status === "rejected") {
      state.inventoryError =
        inventory.status === "rejected"
          ? inventory.reason.message
          : "Chưa kiểm tra được lịch thuê. Bạn vẫn có thể chọn dòng xe và thử lại.";
    } else
      state.cars = inventory.value.filter(
        (c) =>
          c.published &&
          c.status === "AVAILABLE" &&
          !calendar.value.unavailableCarIds.includes(c.id),
      );
    renderStorefrontResults();
  }
  async function viewModel(id) {
    const model = state.catalog.find((m) => m.id === id);
    if (!model) throw new Error("Không tìm thấy dòng xe.");
    const view = modelPresentation(model);
    const filter = { ...state.filters, city: state.filters.city || view.city };
    showModal(
      model.name,
      `<div class="gallery model-gallery">${vehicleImage(model.imageUrl, model.name)}</div><div class="row spread"><div><p class="eyebrow">${esc(model.brand)}</p><h3>${esc(model.name)}</h3><p class="muted">${model.seatCount} chỗ</p></div><div class="model-rate"><span class="model-location">${esc(view.city)}</span><div class="price model-price">${money(view.pricePerDay)} <small>/ngày</small></div></div></div>
      <form id="model-search-form" class="form-grid" data-model="${esc(model.id)}">${provinceSelect("Địa điểm nhận xe", "city", filter.city)}<div></div>${input("Nhận xe", "startsAt", state.filters.startsAt, "datetime-local", "required")}${input("Trả xe", "endsAt", state.filters.endsAt, "datetime-local", "required")}<div class="full"><button type="submit" class="primary">Tìm xe dòng này</button></div></form><div id="model-matches" style="margin-top:22px"><p class="loading">Đang kiểm tra xe theo lịch…</p></div>`,
    );
    try {
      await modelMatches(model, filter);
    } catch (err) {
      if ($("#model-matches"))
        $("#model-matches").innerHTML =
          `<p class="notice">${esc(err.message)}</p>`;
    }
  }
  async function modelMatches(model, filter) {
    const params = new URLSearchParams({ model: model.id });
    if (filter.city) params.set("city", filter.city);
    for (const k of ["maxPrice", "seats"])
      if (filter[k]) params.set(k, filter[k]);
    const [listed, calendar] = await Promise.all([
      api("car", "/api/cars?" + params),
      api(
        "rental",
        "/api/availability?" +
          new URLSearchParams({
            startsAt: new Date(filter.startsAt).toISOString(),
            endsAt: new Date(filter.endsAt).toISOString(),
          }),
      ),
    ]);
    const available = listed.filter(
      (c) =>
        c.published &&
        c.status === "AVAILABLE" &&
        !calendar.unavailableCarIds.includes(c.id),
    );
    if (!$("#model-matches")) return;
    $("#model-matches").innerHTML = available.length
      ? `<h3>Xe đang cho thuê</h3><div class="model-matches-list">${available.map((c) => carCard(c)).join("")}</div>`
      : `<p class="notice">Chưa có ${esc(model.name)} được đăng cho thuê tại ${esc(filter.city || "khu vực đã chọn")} trong thời gian này. Hãy chọn địa điểm hoặc lịch khác.</p>`;
  }
  async function viewCar(id) {
    const c = await api("car", "/api/cars/" + id);
    state.selected = c;
    state.quote = null;
    showModal(
      c.name,
      `<div class="gallery">${c.imageUrls.map((u) => `<img src="${esc(u)}" alt="${esc(c.name)}">`).join("")}</div><div class="detail-cols"><section><span class="badge">${esc(c.city)} · ${esc(c.district)}</span><h3 style="margin-top:18px">Thông tin xe</h3><dl><dt>Số chỗ</dt><dd>${c.seatCount}</dd><dt>Năm sản xuất</dt><dd>${esc(c.modelYear)}</dd><dt>Hộp số</dt><dd>${c.transmission === "MANUAL" ? "Số sàn" : "Số tự động"}</dd><dt>Biển số</dt><dd>${esc(c.licensePlate)}</dd></dl><p class="long">${esc(c.description)}</p><p class="hint">Địa chỉ bàn giao cụ thể được cung cấp sau khi chuyến được xác nhận. Không bao gồm nhiên liệu, phí đường bộ hoặc chi phí phát sinh thỏa thuận ngoài giá thuê.</p></section><section class="panel"><div class="price">${money(c.pricePerDay)} <small>/ngày</small></div><form id="quote-form" class="stack" style="margin-top:20px">${input("Nhận xe", "startsAt", state.filters.startsAt, "datetime-local", "required")}${input("Trả xe", "endsAt", state.filters.endsAt, "datetime-local", "required")}<button class="primary" type="submit">Kiểm tra lịch & chi phí</button></form><div id="quote-result"></div></section></div>`,
    );
  }
  async function quote(form) {
    const v = Object.fromEntries(new FormData(form)),
      q = await api(
        "rental",
        "/api/quotes?" +
          new URLSearchParams({
            carId: state.selected.id,
            startsAt: new Date(v.startsAt).toISOString(),
            endsAt: new Date(v.endsAt).toISOString(),
          }),
      );
    state.quote = { ...q, key: crypto.randomUUID() };
    $("#quote-result").innerHTML =
      `<dl><dt>${q.days} ngày × ${money(q.pricePerDay)}</dt><dd>${money(q.totalPrice)}</dd><dt>Giữ chỗ (${q.depositPercent}%)</dt><dd>${money(q.deposit)}</dd><dt class="total">Tổng tiền thuê</dt><dd class="total">${money(q.totalPrice)}</dd></dl><p class="hint">Thời lượng được làm tròn lên từng 24 giờ. Yêu cầu cần chủ xe duyệt; chưa phát sinh thanh toán khi gửi.</p>${role() === "CUSTOMER" ? `<form id="booking-form" class="stack"><label>Lời nhắn cho chủ xe<textarea name="message" maxlength="1000" placeholder="Nhu cầu nhận xe, hành trình dự kiến…"></textarea></label><label class="row"><input type="checkbox" name="accept" required>Tôi đồng ý với tổng tiền và điều kiện thuê của xe.</label><button type="submit" class="primary">Gửi yêu cầu thuê xe</button></form>` : !state.user ? button("Đăng nhập để đặt xe", "login", "", "primary") : '<p class="notice">Sử dụng tài khoản khách thuê để gửi yêu cầu.</p>'}`;
  }
  async function mycars() {
    state.cars = await api("car", "/api/cars?mine=true");
    $("#app").innerHTML =
      head(
        "Xe của tôi",
        "Thêm hoặc cập nhật xe để hiển thị ngay cho khách thuê.",
        button("Đăng xe mới", "new-car", "", "primary"),
      ) +
      `<div class="grid">${state.cars.map((c) => carCard(c, true)).join("") || empty("Bắt đầu với chiếc xe của bạn", "Chuẩn bị ảnh xe, giá thuê, địa điểm và thông tin bàn giao.", button("Đăng xe đầu tiên", "new-car", "", "primary"))}</div>`;
  }
  async function editCar(id) {
    await directories();
    const c = id ? await api("car", "/api/cars/" + id) : {};
    state.editing = c;
    state.photos = [...(c.photoIds || [])];
    showModal(
      id ? "Chỉnh sửa thông tin xe" : "Đăng xe cho thuê",
      `<form id="car-form" class="form-grid"><div class="full">${select("Chọn dòng xe", "modelSelection", [["", "Tự nhập thông tin xe"], ...state.catalog.map((m) => [m.id, m.name])], modelForCar(c)?.id || "")}</div>${input("Tên xe", "name", c.name || "", "text", 'required maxlength="150" placeholder="Ví dụ: Toyota Vios 2023"')}${input("Hãng xe", "brand", c.brand || "", "text", 'required maxlength="100" list="brands"')}<datalist id="brands">${["VinFast", "Toyota", "Hyundai", "Kia", "Mazda", "Honda", "Ford", "Mitsubishi", "Mercedes-Benz"].map((b) => `<option>${b}</option>`).join("")}</datalist>${input("Biển số", "licensePlate", c.licensePlate || "", "text", 'required maxlength="20"')}${input("Năm sản xuất", "modelYear", c.modelYear || "", "number", `required min="1990" max="${new Date().getFullYear() + 1}"`)}${input("Số chỗ", "seatCount", c.seatCount || 5, "number", 'required min="2" max="16"')}${input("Giá thuê / 24 giờ (VND)", "pricePerDay", c.pricePerDay || "", "number", 'required min="1" step="1"')}${select(
        "Hộp số",
        "transmission",
        [
          ["AUTOMATIC", "Số tự động"],
          ["MANUAL", "Số sàn"],
        ],
        c.transmission,
      )}${select(
        "Nhiên liệu",
        "fuelType",
        [
          ["PETROL", "Xăng"],
          ["DIESEL", "Dầu"],
          ["ELECTRIC", "Điện"],
          ["HYBRID", "Hybrid"],
        ],
        c.fuelType,
      )}${provinceSelect("Tỉnh / thành phố", "city", c.city, true)}${input("Phường / xã, khu vực", "district", c.district || "", "text", 'required maxlength="150"')}${select(
        "Trạng thái tiếp nhận khách",
        "status",
        [
          ["AVAILABLE", "Sẵn sàng cho thuê"],
          ["MAINTENANCE", "Bảo dưỡng"],
          ["INACTIVE", "Ngừng cho thuê"],
        ],
        c.status || "AVAILABLE",
      )}<label class="full">Địa chỉ nhận xe (chỉ chủ xe / quản trị viên thấy)<input name="pickupAddress" value="${esc(c.pickupAddress || "")}" required maxlength="500"></label><label class="full">Mô tả, tiện ích và điều kiện thuê<textarea name="description" required maxlength="3000">${esc(c.description || "")}</textarea></label><div class="full"><h3>Ảnh xe của bạn</h3><p class="file-note">Có thể dùng ảnh của dòng xe đã chọn hoặc tải ảnh xe của bạn. Tối đa 6 ảnh JPEG / PNG, 8 MB/ảnh, từ 200 × 150 px. Không đưa giấy tờ và thông tin cá nhân vào ảnh.</p><div class="thumbs" id="photo-list"></div><label style="margin-top:16px">Thêm ảnh<input id="photo-files" type="file" accept="image/jpeg,image/png" multiple></label><p id="photo-progress" class="hint"></p></div><p class="notice full">Lưu thông tin để đăng xe ngay. Xe ở trạng thái Sẵn sàng sẽ xuất hiện khi khách tìm lịch phù hợp.</p><div class="actions full"><button type="submit" class="primary">${id ? "Lưu thay đổi" : "Thêm và đăng xe"}</button>${button("Đóng", "close")}</div></form>`,
    );
    renderPhotos();
  }
  function renderPhotos() {
    if ($("#photo-list"))
      $("#photo-list").innerHTML = state.photos
        .map(
          (id) =>
            `<div class="thumb"><img src="/media/${esc(id)}" alt="Ảnh xe đã tải">${button("×", "remove-photo", id)}</div>`,
        )
        .join("");
    if ($("#photo-list") && state.photos.length === 0) {
      const form = $("#car-form");
      const model =
        state.catalog.find(
          (m) => m.id === form?.elements.modelSelection.value,
        ) || modelForCar(state.editing || {});
      const src =
        model?.imageUrl ||
        state.editing?.imageUrl ||
        "/images/fleet-garage.png";
      $("#photo-list").innerHTML =
        `<div class="owner-photo-preview">${vehicleImage(src, form?.elements.name.value || model?.name || "Xe của bạn")}</div>`;
    }
  }
  async function uploadFiles(input) {
    const files = [...input.files];
    if (state.photos.length + files.length > 6)
      throw new Error("Mỗi xe có tối đa 6 ảnh.");
    input.disabled = true;
    try {
      for (let i = 0; i < files.length; i++) {
        if (
          !["image/jpeg", "image/png"].includes(files[i].type) ||
          files[i].size > 8 * 1024 * 1024
        )
          throw new Error("Chọn ảnh JPEG / PNG không quá 8 MB.");
        $("#photo-progress").textContent =
          `Đang tải ảnh ${i + 1}/${files.length}…`;
        const form = new FormData();
        form.append("file", files[i]);
        const p = await api("car", "/api/car-media", {
          method: "POST",
          body: form,
        });
        state.photos.push(p.id);
        renderPhotos();
      }
      $("#photo-progress").textContent = "Đã tải ảnh. Nhấn Lưu để cập nhật xe.";
    } finally {
      input.disabled = false;
      input.value = "";
    }
  }
  function tripCard(b) {
    return `<article class="trip">${b.imageUrl ? `<img src="${esc(b.imageUrl)}" alt="${esc(b.carName)}">` : "<div></div>"}<div><div class="row"><h3>${esc(b.carName)} · #${b.id}</h3>${badge(b.status)}</div><p>${date(b.startsAt)} → ${date(b.endsAt)}</p><p class="muted">${b.days} ngày · Đã thực nhận ${money(b.paid)}</p></div><div><div class="price">${money(b.totalPrice)}</div>${button("Chi tiết chuyến", "trip", b.id)}</div></article>`;
  }
  async function trips() {
    state.trips = await api("rental", "/api/bookings");
    const b = state.trips,
      active = b.filter((x) =>
        ["PENDING", "APPROVED", "CONFIRMED", "IN_PROGRESS"].includes(x.status),
      );
    $("#app").innerHTML =
      head(
        role() === "OWNER"
          ? "Yêu cầu & chuyến xe"
          : role() === "ADMIN"
            ? "Quản lý đơn thuê"
            : "Chuyến của tôi",
        "Theo dõi trạng thái, thanh toán và lịch sử xử lý của từng chuyến.",
      ) +
      `<div class="metrics"><div class="metric">Tổng chuyến<strong>${b.length}</strong></div><div class="metric">Đang xử lý<strong>${active.length}</strong></div><div class="metric">Hoàn tất<strong>${b.filter((x) => x.status === "COMPLETED").length}</strong></div></div>${b.map(tripCard).join("") || empty("Chưa có chuyến xe", role() === "OWNER" ? "Yêu cầu thuê sẽ xuất hiện khi khách đặt xe của bạn." : "Chọn xe và thời gian phù hợp để bắt đầu hành trình.", button("Tìm xe", "go-cars", "", "primary"))}`;
  }
  async function trip(id) {
    const b = await api("rental", "/api/bookings/" + id);
    state.booking = b;
    const own = String(state.user.id) === b.ownerId || role() === "ADMIN",
      customer = String(state.user.id) === b.customerId;
    let instructions = null;
    if (
      ["APPROVED", "CONFIRMED", "IN_PROGRESS", "COMPLETED"].includes(b.status)
    )
      instructions = await api(
        "rental",
        `/api/bookings/${id}/payment-instructions`,
      );
    showModal(
      "Chuyến xe #" + b.id,
      `<div class="row spread"><h3>${esc(b.carName)}</h3>${badge(b.status)}</div><dl><dt>Nhận xe</dt><dd>${date(b.startsAt)}</dd><dt>Trả xe</dt><dd>${date(b.endsAt)}</dd><dt>Tổng tiền (${b.days} ngày)</dt><dd>${money(b.totalPrice)}</dd><dt>Cần thanh toán giữ chỗ</dt><dd>${money(b.deposit)}</dd><dt>Đã thực nhận</dt><dd>${money(b.paid)}</dd><dt>Còn phải thanh toán</dt><dd>${money(b.remaining)}</dd>${b.paymentDeadline && b.status === "APPROVED" ? `<dt>Hạn thanh toán</dt><dd>${date(b.paymentDeadline)}</dd>` : ""}${b.handoverAddress ? `<dt>Bàn giao xe</dt><dd>${esc(b.handoverAddress)}</dd>` : ""}</dl>${b.message ? `<p class="notice">Lời nhắn: ${esc(b.message)}</p>` : ""}${b.reason ? `<p class="notice">${esc(b.reason)}</p>` : ""}<div class="actions">${own && b.status === "PENDING" ? button("Duyệt yêu cầu", "booking-action", "approve", "primary") + button("Từ chối", "booking-action", "reject", "danger") : ""}${(own || customer) && ["PENDING", "APPROVED", "CONFIRMED"].includes(b.status) ? button("Hủy chuyến", "booking-action", "cancel", "danger") : ""}${own && b.status === "CONFIRMED" ? button("Xác nhận bàn giao xe", "booking-action", "start", "primary") : ""}${own && b.status === "IN_PROGRESS" ? button("Xác nhận trả xe & hoàn tất", "booking-action", "complete", "primary") : ""}</div>${instructions ? `<section class="panel"><h3>Hướng dẫn thanh toán</h3>${instructions.method === "BANK_TRANSFER" ? `<dl><dt>Ngân hàng</dt><dd>${esc(instructions.bankName)}</dd><dt>Số tài khoản</dt><dd>${esc(instructions.accountNumber)}</dd><dt>Người nhận</dt><dd>${esc(instructions.accountHolder)}</dd><dt>Nội dung chuyển khoản</dt><dd>VELODRIVE ${id}</dd></dl><p class="hint">Sau khi chuyển khoản, gửi mã giao dịch. Chuyến chỉ xác nhận khi chủ xe kiểm tra và ghi nhận đã nhận tiền.</p>` : "<p>Thanh toán trực tiếp với chủ xe. Chủ xe ghi nhận số tiền đã thực nhận tại đây.</p>"}${b.remaining > 0 && ["APPROVED", "CONFIRMED", "IN_PROGRESS"].includes(b.status) ? `${customer && instructions.method === "BANK_TRANSFER" ? button("Gửi thông tin chuyển khoản", "payment", "bank", "primary") : ""}${own ? button("Ghi nhận tiền mặt đã nhận", "payment", "cash", "primary") : ""}` : ""}</section>` : ""}${b.refundDue > 0 ? `<p class="notice">Cần hoàn lại ${money(b.refundDue)}. ${b.refundedAt ? "Đã ghi nhận hoàn tiền " + date(b.refundedAt) + " · " + esc(b.refundReference) : "Đang chờ chủ xe thực hiện và xác nhận hoàn tiền."}</p>${own && !b.refundedAt ? button("Xác nhận đã hoàn tiền", "booking-action", "refund") : ""}` : ""}<section class="panel"><h3>Giao dịch</h3>${b.payments.length ? b.payments.map((p) => `<div class="notice" style="margin-bottom:10px"><div class="row spread"><strong>${money(p.amount)} · ${p.method === "CASH" ? "Tiền mặt" : "Chuyển khoản"}</strong>${badge(p.status)}</div><p class="hint">${esc(p.reference)} · ${date(p.createdAt)}${p.note ? " · " + esc(p.note) : ""}</p>${own && p.status === "PENDING" && ["APPROVED", "CONFIRMED", "IN_PROGRESS"].includes(b.status) ? `<div class="actions">${button("Đã kiểm tra & nhận đủ số tiền này", "verify-payment", p.id, "primary")}${button("Chưa nhận được tiền", "reject-payment", p.id)}</div>` : ""}</div>`).join("") : '<p class="muted">Chưa có giao dịch được ghi nhận.</p>'}</section><h3>Lịch sử xử lý</h3><div class="timeline">${b.events.map((e) => `<p><strong>${esc(e.action)}</strong> · ${date(e.createdAt)}<br>${esc(e.note || "")}</p>`).join("")}</div>`,
    );
  }
  function actionForm(action) {
    const labels = {
      approve: "Duyệt yêu cầu thuê",
      reject: "Từ chối yêu cầu",
      cancel: "Hủy chuyến",
      start: "Xác nhận đã bàn giao xe",
      complete: "Xác nhận đã nhận lại xe",
      refund: "Xác nhận đã hoàn tiền",
    };
    const id = state.booking.id;
    showModal(
      labels[action],
      `<form id="action-form" class="stack" data-booking="${id}" data-action-name="${action}">${action === "approve" ? '<p class="notice">Duyệt sẽ giữ lịch xe đến hạn thanh toán. Kiểm tra khả năng bàn giao trước khi xác nhận.</p>' : ""}${["approve", "reject", "cancel", "refund"].includes(action) ? `<label>${action === "approve" ? "Địa chỉ và hướng dẫn bàn giao" : action === "refund" ? "Mã giao dịch / biên nhận hoàn tiền thực tế" : "Lý do"}<textarea name="note" required minlength="4" maxlength="${action === "refund" ? 100 : 500}"></textarea></label>` : '<p class="notice">Chỉ xác nhận khi việc bàn giao thực tế đã hoàn thành. Hệ thống lưu thời điểm và người xác nhận.</p>'}${action === "refund" ? '<p class="notice">Thao tác này chỉ ghi nhận khoản hoàn tiền đã thực hiện; hệ thống không tự chuyển tiền.</p>' : ""}<button class="primary" type="submit">${labels[action]}</button>${button("Quay lại chuyến", "trip", id)}</form>`,
    );
  }
  function paymentForm(method) {
    const b = state.booking;
    showModal(
      method === "cash"
        ? "Ghi nhận tiền mặt đã nhận"
        : "Thông tin chuyển khoản",
      `<form id="payment-form" class="stack" data-booking="${b.id}" data-method="${method}"><p class="notice">${method === "cash" ? "Chỉ ghi nhận số tiền bạn đã thực nhận từ khách." : "Chỉ gửi thông tin sau khi đã thực hiện chuyển khoản. Chủ xe sẽ kiểm tra tiền đến trước khi xác nhận."}</p>${input("Số tiền (VND)", "amount", b.paid < b.deposit ? Math.min(b.deposit - b.paid, b.remaining) : b.remaining, "number", `required min="1" step="1" max="${b.remaining}"`)}${input("Mã giao dịch / số biên nhận", "reference", "", "text", 'required minlength="4" maxlength="100"')}<label>Ghi chú<textarea name="note" maxlength="500"></textarea></label><button type="submit" class="primary">${method === "cash" ? "Xác nhận đã nhận tiền" : "Gửi chủ xe kiểm tra"}</button>${button("Quay lại chuyến", "trip", b.id)}</form>`,
    );
  }
  async function billing() {
    const b = await api("rental", "/api/owner/payment-settings");
    $("#app").innerHTML =
      head(
        "Nhận thanh toán",
        "Thiết lập cách khách thanh toán cho những chuyến xe của bạn.",
      ) +
      `<section class="panel"><p class="notice">Hiện tại: ${b.method === "BANK_TRANSFER" ? esc(b.bankName) + " · " + esc(b.accountNumber) + " · " + esc(b.accountHolder) : "Nhận tiền mặt trực tiếp"}. Thông tin tài khoản đầy đủ chỉ hiển thị cho các bên liên quan sau khi yêu cầu thuê được duyệt.</p><form id="billing-form" class="form-grid">${select(
        "Phương thức",
        "method",
        [
          ["CASH", "Tiền mặt trực tiếp"],
          ["BANK_TRANSFER", "Chuyển khoản ngân hàng"],
        ],
        b.method,
      )}<div></div>${input("Ngân hàng", "bankName", "", "text", 'maxlength="100" placeholder="Nhập lại khi đổi cấu hình chuyển khoản"')}${input("Số tài khoản", "accountNumber", "", "text", 'maxlength="40"')}${input("Chủ tài khoản", "accountHolder", "", "text", 'maxlength="150"')}<div class="full"><button type="submit" class="primary">Lưu phương thức thanh toán</button></div></form></section>`;
  }
  async function vehicles() {
    const list = await api("car", "/api/cars");
    $("#app").innerHTML =
      head(
        "Quản lý xe",
        "Xem, cập nhật hoặc ngừng cho thuê xe trong hệ thống.",
        button("Thêm xe", "new-car", "", "primary"),
      ) +
      `<div class="grid">${list.map((c) => carCard(c, true)).join("") || empty("Chưa có xe", "Xe được hiển thị ngay khi chủ xe lưu đủ thông tin.")}</div>`;
  }
  async function accounts() {
    const list = await api("auth", "/api/admin/accounts");
    $("#app").innerHTML =
      head(
        "Quản lý tài khoản",
        "Thông tin cá nhân được che. Không hiển thị mật khẩu hay mã băm.",
      ) +
      `<div class="panel table-wrap"><table><thead><tr><th>Tên đăng nhập</th><th>Vai trò</th><th>Họ tên</th><th>Điện thoại</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>${list.map((a) => `<tr><td>${esc(a.username)}</td><td>${esc(labels[a.role])}</td><td>${esc(a.displayNameMasked || "—")}</td><td>${esc(a.phoneMasked || "—")}</td><td>${a.enabled ? "Hoạt động" : "Đã khóa"}</td><td>${a.role !== "ADMIN" ? button(a.enabled ? "Khóa" : "Mở khóa", "toggle-account", a.id + "-" + a.enabled) : "Quản trị viên"}</td></tr>`).join("")}</tbody></table></div>`;
  }
  async function profile() {
    const p = await api("auth", "/api/auth/me");
    $("#app").innerHTML =
      head(
        "Tài khoản của tôi",
        "Thông tin được che khi hiển thị. Chỉ điền trường bạn muốn cập nhật.",
      ) +
      `<div class="detail-cols"><section class="panel"><h3>Thông tin cá nhân</h3><dl><dt>Tên đăng nhập</dt><dd>${esc(p.username)}</dd><dt>Họ tên</dt><dd>${esc(p.displayNameMasked || "—")}</dd><dt>Điện thoại</dt><dd>${esc(p.phoneMasked || "—")}</dd><dt>Email</dt><dd>${esc(p.emailMasked || "—")}</dd></dl><form id="profile-form" class="stack">${input("Họ tên mới", "displayName", "", "text", 'maxlength="100"')}${input("Điện thoại mới", "phone", "", "tel")}${input("Email mới", "email", "", "email", 'maxlength="254"')}<button type="submit" class="primary">Cập nhật thông tin</button></form></section><section class="panel"><h3>Đổi mật khẩu</h3><form id="password-form" class="stack">${input("Mật khẩu hiện tại", "currentPassword", "", "password", 'required autocomplete="current-password"')}${input("Mật khẩu mới", "newPassword", "", "password", 'required minlength="6" maxlength="72" autocomplete="new-password"')}<p class="hint">Sau khi đổi mật khẩu, hãy đăng nhập lại. Mật khẩu phải có từ 6 ký tự.</p><button type="submit" class="primary">Đổi mật khẩu</button></form></section></div>`;
  }
  async function route() {
    nav();
    const name = routeName();
    if (name !== "cars" && !state.user) {
      $("#app").innerHTML = empty(
        "Đăng nhập để tiếp tục",
        "Quản lý xe, yêu cầu thuê và tài khoản trong không gian riêng của bạn.",
        button("Đăng nhập", "login", "", "primary"),
      );
      return;
    }
    const allowed = {
      mycars: ["OWNER"],
      billing: ["OWNER"],
      vehicles: ["ADMIN"],
      accounts: ["ADMIN"],
    };
    if (allowed[name] && !allowed[name].includes(role())) {
      $("#app").innerHTML = empty(
        "Không có quyền truy cập",
        "Chức năng này thuộc vai trò khác.",
      );
      return;
    }
    $("#app").innerHTML = '<p class="loading">Đang tải dữ liệu…</p>';
    try {
      await directories();
      await (
        { cars, mycars, trips, billing, vehicles, accounts, profile }[name] ||
        cars
      )();
    } catch (e) {
      $("#app").innerHTML = empty(
        "Chưa tải được nội dung",
        e.message,
        button("Thử lại", "refresh"),
      );
    }
  }
  document.addEventListener("click", async (e) => {
    const b = e.target.closest("[data-action]");
    if (!b) return;
    const a = b.dataset.action,
      id = b.dataset.id;
    try {
      b.disabled = true;
      switch (a) {
        case "close":
          $("#modal").close();
          break;
        case "login":
          auth();
          break;
        case "register":
          auth("register");
          break;
        case "logout":
          logout();
          break;
        case "refresh":
          await route();
          break;
        case "view-model":
          await viewModel(id);
          break;
        case "clear-model":
          state.filters.model = "";
          await cars();
          break;
        case "reset-search":
          state.filters = {
            startsAt: state.filters.startsAt,
            endsAt: state.filters.endsAt,
          };
          await cars();
          break;
        case "view-car":
          await viewCar(id);
          break;
        case "go-cars":
          $("#modal").close();
          location.hash = "cars";
          break;
        case "new-car":
          await editCar();
          break;
        case "edit-car":
          await editCar(id);
          break;
        case "remove-photo":
          state.photos = state.photos.filter((x) => x !== id);
          renderPhotos();
          break;
        case "publish-car":
          await api("car", `/api/cars/${id}/publish`, { method: "POST" });
          toast("Xe đã được đăng.");
          await route();
          break;
        case "archive-car":
          showModal(
            "Lưu trữ xe",
            `<p>Xe sẽ ngừng hiển thị cho khách. Các chuyến đã đặt vẫn được lưu và cần xử lý.</p><div class="actions">${button("Xác nhận lưu trữ", "confirm-archive", id, "danger")}${button("Giữ xe", "close")}</div>`,
          );
          break;
        case "confirm-archive":
          await api("car", `/api/cars/${id}`, { method: "DELETE" });
          $("#modal").close();
          toast("Đã lưu trữ xe.");
          await route();
          break;
        case "trip":
          await trip(id);
          break;
        case "booking-action":
          actionForm(id);
          break;
        case "payment":
          paymentForm(id);
          break;
        case "verify-payment":
        case "reject-payment": {
          const bid = state.booking.id;
          showModal(
            a === "verify-payment"
              ? "Xác nhận tiền đã thực nhận"
              : "Từ chối thông tin chuyển khoản",
            `<form id="verify-form" class="stack" data-booking="${bid}" data-payment="${id}" data-accepted="${a === "verify-payment"}"><p class="notice">${a === "verify-payment" ? "Hãy kiểm tra tài khoản ngân hàng và số tiền thực nhận trước khi xác nhận." : "Ghi lý do để khách biết cần kiểm tra hoặc gửi lại giao dịch."}</p><label>Ghi chú<textarea name="note" maxlength="500" ${a === "reject-payment" ? "required" : ""}></textarea></label><button type="submit" class="primary">Xác nhận</button>${button("Quay lại chuyến", "trip", bid)}</form>`,
          );
          break;
        }
        case "toggle-account": {
          const [account, enabled] = id.split("-");
          await api("auth", `/api/admin/accounts/${account}/enabled`, {
            method: "PUT",
            body: { enabled: enabled !== "true" },
          });
          toast("Đã cập nhật trạng thái tài khoản.");
          await accounts();
          break;
        }
      }
    } catch (ex) {
      if ($("#modal").open) error($("#modal-body"), ex);
      else toast(ex.message);
    } finally {
      b.disabled = false;
    }
  });
  document.addEventListener("change", async (e) => {
    if (e.target.name === "modelSelection" && e.target.closest("#car-form")) {
      const m = state.catalog.find((x) => x.id === e.target.value);
      if (m) {
        const form = $("#car-form");
        form.elements.name.value = m.name;
        form.elements.brand.value = m.brand;
        form.elements.seatCount.value = m.seatCount;
      }
      renderPhotos();
    }
    if (
      ["name", "brand"].includes(e.target.name) &&
      e.target.closest("#car-form")
    )
      renderPhotos();
    if (e.target.id === "photo-files") {
      try {
        await uploadFiles(e.target);
      } catch (ex) {
        error($("#car-form"), ex);
      }
    }
    if (e.target.closest("#quote-form")) {
      state.quote = null;
      if ($("#quote-result")) $("#quote-result").innerHTML = "";
    }
  });
  document.addEventListener("submit", async (e) => {
    const form = e.target;
    if (!form.id) return;
    e.preventDefault();
    const submit = e.submitter,
      buttons = $$("button[type=submit]", form);
    buttons.forEach((b) => (b.disabled = true));
    const v = Object.fromEntries(new FormData(form));
    $(".error", form)?.remove();
    try {
      switch (form.id) {
        case "auth-form": {
          if (form.dataset.mode === "register")
            await api("auth", "/api/auth/register", {
              method: "POST",
              body: v,
            });
          const data = await api("auth", "/api/auth/login", {
            method: "POST",
            body: { username: v.username, password: v.password },
          });
          state.token = data.accessToken;
          state.user = data.user;
          $("#modal").close();
          toast(
            form.dataset.mode === "register"
              ? "Đã tạo tài khoản."
              : "Đăng nhập thành công.",
          );
          await route();
          break;
        }
        case "search-form":
          if (new Date(v.endsAt) <= new Date(v.startsAt))
            throw new Error("Thời gian trả phải sau thời gian nhận xe.");
          state.filters = { ...v };
          await cars();
          break;
        case "model-search-form": {
          if (new Date(v.endsAt) <= new Date(v.startsAt))
            throw new Error("Thời gian trả phải sau thời gian nhận xe.");
          state.filters = { ...state.filters, ...v };
          const model = state.catalog.find((m) => m.id === form.dataset.model);
          $("#model-matches").innerHTML = '<p class="loading">Đang tìm xe…</p>';
          await modelMatches(model, state.filters);
          await cars();
          break;
        }
        case "quote-form":
          if (!state.user) {
            auth();
            break;
          }
          await quote(form);
          break;
        case "booking-form": {
          if (!state.quote)
            throw new Error("Vui lòng kiểm tra lại lịch và giá.");
          const q = state.quote,
            b = await api("rental", "/api/bookings", {
              method: "POST",
              headers: { "Idempotency-Key": q.key },
              body: {
                carId: q.carId,
                startsAt: q.startsAt,
                endsAt: q.endsAt,
                expectedTotal: q.totalPrice,
                message: v.message,
              },
            });
          toast("Đã gửi yêu cầu đến chủ xe.");
          location.hash = "trips";
          await trip(b.id);
          break;
        }
        case "car-form": {
          if ($("#photo-files").disabled)
            throw new Error("Chờ tải xong ảnh trước khi lưu.");
          const b = {
            ...v,
            seatCount: Number(v.seatCount),
            modelYear: Number(v.modelYear),
            pricePerDay: Number(v.pricePerDay),
            photoIds: state.photos,
          };
          const c = state.editing.id;
          await api("car", "/api/cars" + (c ? "/" + c : ""), {
            method: c ? "PUT" : "POST",
            body: b,
          });
          $("#modal").close();
          toast(
            "Xe đã được đăng. Khách có thể tìm xe theo địa điểm và lịch thuê.",
          );
          await route();
          break;
        }
        case "action-form":
          await api(
            "rental",
            `/api/bookings/${form.dataset.booking}/actions/${form.dataset.actionName}`,
            { method: "POST", body: { note: v.note || null } },
          );
          toast("Đã cập nhật chuyến xe.");
          await trip(form.dataset.booking);
          await trips();
          break;
        case "payment-form":
          await api(
            "rental",
            `/api/bookings/${form.dataset.booking}/${form.dataset.method === "cash" ? "cash-receipts" : "payment-claims"}`,
            { method: "POST", body: { ...v, amount: Number(v.amount) } },
          );
          toast(
            form.dataset.method === "cash"
              ? "Đã ghi nhận tiền thực nhận."
              : "Đã gửi chủ xe kiểm tra giao dịch.",
          );
          await trip(form.dataset.booking);
          await trips();
          break;
        case "verify-form":
          await api(
            "rental",
            `/api/bookings/${form.dataset.booking}/payments/${form.dataset.payment}/verify`,
            {
              method: "POST",
              body: {
                accepted: form.dataset.accepted === "true",
                note: v.note,
              },
            },
          );
          await trip(form.dataset.booking);
          await trips();
          break;
        case "billing-form":
          await api("rental", "/api/owner/payment-settings", {
            method: "PUT",
            body: v,
          });
          toast("Đã lưu thông tin nhận thanh toán.");
          await billing();
          break;
        case "profile-form":
          await api("auth", "/api/auth/me", {
            method: "PUT",
            body: Object.fromEntries(
              Object.entries(v).filter(([, value]) => value.trim()),
            ),
          });
          toast("Đã cập nhật hồ sơ.");
          await profile();
          break;
        case "password-form":
          await api("auth", "/api/auth/password", { method: "PUT", body: v });
          logout();
          toast("Đã đổi mật khẩu. Hãy đăng nhập lại.");
          auth();
          break;
      }
    } catch (ex) {
      error(form, ex);
    } finally {
      buttons.forEach((b) => (b.disabled = false));
    }
  });
  document.addEventListener(
    "error",
    (e) => {
      const img = e.target;
      if (
        img.matches?.("img[data-car-image]") &&
        img.getAttribute("src") !== "/images/fleet-garage.png"
      )
        img.src = "/images/fleet-garage.png";
    },
    true,
  );
  window.addEventListener("hashchange", route);
  $("#modal").addEventListener("click", (e) => {
    if (e.target === $("#modal")) $("#modal").close();
  });
  route();
})();
