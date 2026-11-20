(function () {
  const cfg = Object.assign(
    {
      businessName: "نشاطك التجاري",
      businessType: "مقهى ومطعم",
      logoUrl: "",
      colors: { primary: "#0F766E", secondary: "#D97706", background: "#FAF7F2" },
      welcomeMessage: "مرحباً بك في برنامج الولاء!",
      termsText: "",
      privacyText: "",
      currency: "MAD",
      pointsPerMad: 10,
      defaultLang: "ar",
      appsScriptUrl: "",
      customerPortalUrl: "",
    },
    window.LOYALTY_CONFIG || {}
  );

  // تطبيق ألوان الهوية البصرية ديناميكياً عبر متغيرات CSS
  function applyThemeColors(config) {
    const root = document.documentElement;
    root.style.setProperty("--primary", config.colors.primary);
    root.style.setProperty("--secondary", config.colors.secondary);
    root.style.setProperty("--bg", config.colors.background);
    document.getElementById("brandTitle").textContent = config.businessName;
    document.getElementById("brandSubtitle").textContent = config.businessType;
    document.title = config.businessName;

    const logoBox = document.getElementById("brandLogoBox");
    if (config.logoUrl) {
      logoBox.innerHTML = `<img src="${config.logoUrl}" alt="logo" style="width:100%;height:100%;border-radius:14px;object-fit:cover"/>`;
    } else {
      logoBox.textContent = (config.businessName || "★").trim().charAt(0);
    }
  }

  let lang = localStorage.getItem("loyalty_lang") || cfg.defaultLang;
  let dark = localStorage.getItem("loyalty_dark") === "1";
  let adminToken = sessionStorage.getItem("loyalty_admin_token") || "";
  let savedPhone = localStorage.getItem("loyalty_customer_phone") || "";
  let deferredPrompt = null;

  const DEMO_KEY = "loyalty_demo_db_v1";
  function loadDemoDb() {
    const raw = localStorage.getItem(DEMO_KEY);
    if (raw) return JSON.parse(raw);
    const seed = {
      customers: [
        { id: "CUS-101", name: "سلمى الإدريسي", phone: "0661234567", email: "salma@example.com", points: 145, lastVisit: new Date().toISOString(), notificationConsent: true },
        { id: "CUS-102", name: "يوسف بنعلي", phone: "0662345678", email: "youssef@example.com", points: 75, lastVisit: new Date().toISOString(), notificationConsent: true },
        { id: "CUS-103", name: "خديجة العمراني", phone: "0663456789", email: "khadija@example.com", points: 220, lastVisit: new Date().toISOString(), notificationConsent: false },
      ],
      rewards: [
        { id: "RWD-1", title: "قهوة أو عصير مجاني", pointsRequired: 50, active: true },
        { id: "RWD-2", title: "خصم 25 درهم", pointsRequired: 100, active: true },
        { id: "RWD-3", title: "وجبة خاصة مجانية", pointsRequired: 200, active: true },
      ],
      transactions: [
        { id: "TX-1", date: new Date().toISOString(), customerId: "CUS-101", customerName: "سلمى الإدريسي", type: "إضافة", points: 15, billAmountMad: 150, note: "فاتورة 150 MAD" },
      ],
      promotions: [
        { id: "PRM-1", date: new Date().toISOString(), title: "عرض نهاية الأسبوع", message: "نقاط مضاعفة كل جمعة وسبت!", channel: "both", recipientCount: 2 },
      ],
    };
    localStorage.setItem(DEMO_KEY, JSON.stringify(seed));
    return seed;
  }

  async function apiCall(action, payload = {}) {
    if (!cfg.appsScriptUrl) {
      return handleDemoApi(action, payload);
    }
    const res = await fetch(cfg.appsScriptUrl, {
      method: "POST",
      headers: { "Content-Type": "text/plain;charset=utf-8" },
      body: JSON.stringify(Object.assign({ action }, payload)),
    });
    return await res.json();
  }

  function handleDemoApi(action, data) {
    const db = loadDemoDb();
    if (action === "getPublicConfig") {
      return { ok: true, settings: cfg, rewards: db.rewards.filter((r) => r.active), promotions: db.promotions };
    }
    if (action === "registerCustomer") {
      const phone = String(data.phone || "").replace(/\s+/g, "");
      if (!data.name || phone.length < 8 || !data.notificationConsent) {
        return { ok: false, error: "المرجو ملء الاسم والهاتف والموافقة على الإشعارات" };
      }
      const exists = db.customers.find((c) => c.phone === phone);
      if (exists) return { ok: false, error: "رقم الهاتف مسجل مسبقاً", customer: exists };
      const c = { id: "CUS-" + Math.floor(100 + Math.random() * 900), name: data.name, phone, email: data.email || "", points: 0, lastVisit: new Date().toISOString(), notificationConsent: true };
      db.customers.push(c);
      localStorage.setItem(DEMO_KEY, JSON.stringify(db));
      return { ok: true, customer: c };
    }
    if (action === "getCustomerCard") {
      const c = db.customers.find((x) => x.phone === data.phone);
      if (!c) return { ok: false, error: "البطاقة غير موجودة" };
      return { ok: true, customer: c, rewards: db.rewards.filter((r) => r.active), transactions: db.transactions.filter((t) => t.customerId === c.id), promotions: db.promotions };
    }
    if (action === "verifyAdmin") {
      return data.token === "admin1234" ? { ok: true } : { ok: false, error: "كلمة السر الافتراضية للتجربة: admin1234" };
    }
    if (action === "getDashboard") {
      const totalPoints = db.transactions.filter((t) => t.type === "إضافة").reduce((s, t) => s + t.points, 0);
      return { ok: true, stats: { totalCustomers: db.customers.length, totalPointsDistributed: totalPoints, remainingEmailQuota: 100 }, customers: db.customers, rewards: db.rewards, recentTransactions: db.transactions.slice(-10).reverse() };
    }
    if (action === "addPoints") {
      const target = db.customers.find((c) => c.id === data.customerId || c.phone === data.phone);
      if (!target) return { ok: false, error: "لم يتم العثور على الزبون" };
      const bill = Number(data.billAmountMad || 0);
      const pts = bill > 0 && !data.points ? Math.floor(bill / Math.max(1, cfg.pointsPerMad)) : Number(data.points || 0);
      if (pts <= 0) return { ok: false, error: "عدد النقاط غير صالح" };
      target.points += pts;
      target.lastVisit = new Date().toISOString();
      db.transactions.unshift({
        id: "TX-" + Math.floor(100 + Math.random() * 900),
        date: new Date().toISOString(),
        customerId: target.id,
        customerName: target.name,
        type: "إضافة",
        points: pts,
        billAmountMad: bill,
        note: data.note || (bill > 0 ? `فاتورة ${bill} ${cfg.currency}` : "إضافة عبر NFC"),
      });
      localStorage.setItem(DEMO_KEY, JSON.stringify(db));
      return { ok: true, message: `تمت إضافة ${pts} نقطة لـ ${target.name}`, newPoints: target.points };
    }
    return { ok: true };
  }

  async function renderCustomer() {
    const el = document.getElementById("customerView");
    if (!savedPhone) {
      el.innerHTML = `
        <div class="card">
          <h2>${lang === "ar" ? "بطاقة الولاء الرقمية" : "Carte de Fidélité"}</h2>
          <p style="color:var(--muted)">${cfg.welcomeMessage}</p>
          <input id="regName" class="input" placeholder="${lang === "ar" ? "الاسم الكامل" : "Nom complet"}" />
          <input id="regPhone" class="input" type="tel" placeholder="06XXXXXXXX" />
          <input id="regEmail" class="input" type="email" placeholder="${lang === "ar" ? "البريد الإلكتروني (اختياري)" : "Email (optionnel)"}" />
          <label style="display:flex;gap:10px;align-items:flex-start;margin-bottom:14px;font-size:13px">
            <input id="regConsent" type="checkbox" style="width:20px;height:20px" />
            <span>أوافق على استلام العروض والتخفيضات وأقرّ بالاطلاع على سياسة الخصوصية: ${cfg.privacyText}</span>
          </label>
          <button id="regSubmitBtn" class="btn btn-primary">${lang === "ar" ? "إنشاء بطاقتي" : "Créer ma carte"}</button>
          <hr style="border:none;border-top:1px solid var(--border);margin:16px 0"/>
          <p style="font-size:13px;color:var(--muted)">لديك بطاقة مسبقاً؟</p>
          <div style="display:flex;gap:8px">
            <input id="loginPhone" class="input" style="margin:0" type="tel" placeholder="06XXXXXXXX" />
            <button id="loginPhoneBtn" class="btn btn-secondary">دخول</button>
          </div>
        </div>`;
      document.getElementById("regSubmitBtn").onclick = async () => {
        const res = await apiCall("registerCustomer", {
          name: document.getElementById("regName").value,
          phone: document.getElementById("regPhone").value,
          email: document.getElementById("regEmail").value,
          notificationConsent: document.getElementById("regConsent").checked,
        });
        if (!res.ok && !res.customer) return alert(res.error);
        savedPhone = (res.customer && res.customer.phone) || document.getElementById("regPhone").value;
        localStorage.setItem("loyalty_customer_phone", savedPhone);
        renderCustomer();
      };
      document.getElementById("loginPhoneBtn").onclick = () => {
        savedPhone = document.getElementById("loginPhone").value.trim();
        localStorage.setItem("loyalty_customer_phone", savedPhone);
        renderCustomer();
      };
      return;
    }

    const res = await apiCall("getCustomerCard", { phone: savedPhone });
    if (!res.ok) {
      localStorage.removeItem("loyalty_customer_phone");
      savedPhone = "";
      return renderCustomer();
    }
    const c = res.customer;
    const nextReward = (res.rewards || []).find((r) => r.pointsRequired > c.points) || (res.rewards || [])[0];
    const pct = nextReward ? Math.min(100, Math.round((c.points / nextReward.pointsRequired) * 100)) : 100;

    el.innerHTML = `
      <div class="loyalty-card">
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>${c.name}</span>
          <span style="font-size:12px;opacity:.85">${c.id}</span>
        </div>
        <div class="points-big">${c.points} <span style="font-size:18px;font-weight:600">نقطة</span></div>
        <div class="progress-track"><div class="progress-fill" style="width:${pct}%"></div></div>
        <div style="font-size:13px;opacity:.92">${nextReward ? `المكافأة القادمة: ${nextReward.title} (${nextReward.pointsRequired} نقطة)` : "جميع المكافآت متاحة لك!"}</div>
      </div>
      <div class="card">
        <h3>المكافآت المتاحة</h3>
        ${(res.rewards || []).map((r) => `<div style="display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px solid var(--border)"><span>${r.title}</span><strong>${r.pointsRequired} نقطة</strong></div>`).join("")}
      </div>
      <div class="card">
        <h3>العروض الحالية</h3>
        ${(res.promotions || []).map((p) => `<div style="padding:8px 0;border-bottom:1px solid var(--border)"><strong>${p.title}</strong><div style="font-size:13px;color:var(--muted)">${p.message}</div></div>`).join("") || "<p>لا توجد عروض حالياً</p>"}
      </div>
      <button id="switchCustomerBtn" class="btn btn-outline" style="width:100%">تغيير رقم الهاتف</button>`;
    document.getElementById("switchCustomerBtn").onclick = () => {
      localStorage.removeItem("loyalty_customer_phone");
      savedPhone = "";
      renderCustomer();
    };
  }

  let nfcAbortController = null;
  let selectedAdminCustomer = null;

  function parseCustomerIdentifierFromNfc(raw) {
    const trimmed = String(raw || "").trim();
    try {
      if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        const u = new URL(trimmed);
        const p = u.searchParams.get("phone") || u.searchParams.get("id") || u.searchParams.get("customerId");
        if (p) return p.trim();
      }
    } catch (_) {}
    const cusMatch = trimmed.match(/CUS-[A-Za-z0-9]+/i);
    if (cusMatch) return cusMatch[0].toUpperCase();
    const phoneMatch = trimmed.replace(/\s+/g, "").match(/(?:\+212|212|0)[5-7]\d{8}/);
    if (phoneMatch) return phoneMatch[0];
    return trimmed;
  }

  function decodeNdefRecords(records) {
    const decoder = new TextDecoder();
    for (const record of records || []) {
      if (record.recordType === "text") {
        const textDecoder = new TextDecoder(record.encoding || "utf-8");
        return textDecoder.decode(record.data);
      }
      if (record.recordType === "url" || record.recordType === "absolute-url" || record.recordType === "mime") {
        return decoder.decode(record.data);
      }
    }
    return "";
  }

  async function renderAdmin() {
    const el = document.getElementById("adminView");
    if (!adminToken) {
      el.innerHTML = `
        <div class="card">
          <h2>دخول لوحة المدير</h2>
          <p style="font-size:13px;color:var(--muted)">في الوضع التجريبي كلمة السر هي: <code>admin1234</code></p>
          <input id="adminPassInput" class="input" type="password" placeholder="كلمة سر المدير" />
          <button id="adminLoginBtn" class="btn btn-primary">دخول</button>
        </div>`;
      document.getElementById("adminLoginBtn").onclick = async () => {
        const token = document.getElementById("adminPassInput").value.trim();
        const res = await apiCall("verifyAdmin", { token });
        if (!res.ok) return alert(res.error);
        adminToken = token;
        sessionStorage.setItem("loyalty_admin_token", token);
        renderAdmin();
      };
      return;
    }

    const dash = await apiCall("getDashboard", { token: adminToken });
    const listRes = cfg.appsScriptUrl ? await apiCall("listCustomers", { token: adminToken }) : dash;
    const customers = (listRes && listRes.customers) || dash.customers || [];
    const portalUrl = cfg.customerPortalUrl || window.location.href.split("#")[0];
    const webNfcSupported = "NDEFReader" in window;

    el.innerHTML = `
      <div class="card" id="nfcAdminCard">
        <div style="display:flex;justify-content:space-between;align-items:center;gap:10px">
          <div>
            <strong>قارئ NFC لبطاقات الزبناء</strong>
            <div id="nfcStatusText" style="font-size:13px;color:var(--muted)">
              ${webNfcSupported ? "اضغط لتفعيل NDEFReader وقراءة بطاقة الزبون عند الاقتراب" : "Web NFC غير مدعوم في هذا المتصفح (يعمل على Chrome للأندرويد عبر HTTPS)"}
            </div>
          </div>
          <button id="activateNfcBtn" class="btn btn-secondary" ${webNfcSupported ? "" : "disabled"}>
            ${nfcAbortController ? "إيقاف NFC" : "تفعيل قارئ NFC"}
          </button>
        </div>
      </div>

      ${selectedAdminCustomer ? `
        <div class="card" style="border:2px solid var(--primary)">
          <div style="display:flex;justify-content:space-between;align-items:center">
            <div>
              <strong>${selectedAdminCustomer.name}</strong>
              <div style="font-size:13px;color:var(--muted)">${selectedAdminCustomer.phone} • ${selectedAdminCustomer.id}</div>
            </div>
            <strong style="color:var(--primary);font-size:20px">${selectedAdminCustomer.points} نقطة</strong>
          </div>
          <div style="margin-top:12px">
            <input id="quickBillMad" class="input" type="number" placeholder="مبلغ الفاتورة (${cfg.currency}) - كل ${cfg.pointsPerMad} ${cfg.currency} = 1 نقطة" />
            <input id="quickManualPts" class="input" type="number" placeholder="أو عدد النقاط يدوياً" />
            <div style="display:flex;gap:8px">
              <button id="quickAddPtsBtn" class="btn btn-primary" style="flex:1">إضافة النقاط</button>
              <button id="closeSelectedBtn" class="btn btn-outline">إغلاق</button>
            </div>
          </div>
        </div>
      ` : ""}

      <div class="kpi-grid">
        <div class="kpi"><div class="val">${dash.stats.totalCustomers}</div><div class="lbl">عدد الزبناء</div></div>
        <div class="kpi"><div class="val">${dash.stats.totalPointsDistributed}</div><div class="lbl">النقاط الموزعة</div></div>
        <div class="kpi"><div class="val">${cfg.pointsPerMad} ${cfg.currency}</div><div class="lbl">قيمة النقطة الواحدة</div></div>
        <div class="kpi"><div class="val">${dash.stats.remainingEmailQuota}</div><div class="lbl">حصة البريد اليومية</div></div>
      </div>

      <div class="card">
        <h3>الزبناء (${customers.length})</h3>
        ${customers.map((c) => `
          <div class="admin-cust-row" data-id="${c.id}" style="display:flex;justify-content:space-between;align-items:center;padding:10px 0;border-bottom:1px solid var(--border);cursor:pointer">
            <div><strong>${c.name}</strong><div style="font-size:12px;color:var(--muted)">${c.phone} • ${c.id}</div></div>
            <strong style="color:var(--primary)">${c.points} نقطة</strong>
          </div>`).join("")}
      </div>

      <div class="card">
        <h3>رابط الزبناء (QR & NFC)</h3>
        <input class="input" readonly value="${portalUrl}" />
        <p style="font-size:13px;color:var(--muted)">انسخ هذا الرابط لبرمجة ملصق NFC أو طباعة QR Code على الطاولات.</p>
      </div>`;

    document.querySelectorAll(".admin-cust-row").forEach((row) => {
      row.onclick = () => {
        selectedAdminCustomer = customers.find((c) => c.id === row.dataset.id) || null;
        renderAdmin();
      };
    });

    if (selectedAdminCustomer) {
      document.getElementById("closeSelectedBtn").onclick = () => {
        selectedAdminCustomer = null;
        renderAdmin();
      };
      document.getElementById("quickAddPtsBtn").onclick = async () => {
        const billAmountMad = Number(document.getElementById("quickBillMad").value || 0);
        const manualPts = Number(document.getElementById("quickManualPts").value || 0);
        const res = await apiCall("addPoints", {
          token: adminToken,
          customerId: selectedAdminCustomer.id,
          billAmountMad,
          points: manualPts,
        });
        if (!res.ok) return alert(res.error);
        alert(res.message);
        selectedAdminCustomer = Object.assign({}, selectedAdminCustomer, { points: res.newPoints ?? selectedAdminCustomer.points });
        renderAdmin();
      };
    }

    const nfcBtn = document.getElementById("activateNfcBtn");
    if (nfcBtn && webNfcSupported) {
      nfcBtn.onclick = async () => {
        const statusEl = document.getElementById("nfcStatusText");
        if (nfcAbortController) {
          nfcAbortController.abort();
          nfcAbortController = null;
          renderAdmin();
          return;
        }
        try {
          nfcAbortController = new AbortController();
          const ndef = new window.NDEFReader();
          await ndef.scan({ signal: nfcAbortController.signal });
          nfcBtn.textContent = "إيقاف NFC";
          statusEl.textContent = "قارئ NFC مفعّل — قرّب هاتف أو بطاقة الزبون الآن...";

          ndef.addEventListener("readingerror", () => {
            statusEl.textContent = "تعذّرت قراءة البطاقة، أعد تقريبها.";
          });

          ndef.addEventListener("reading", ({ message, serialNumber }) => {
            const payload = decodeNdefRecords(message && message.records) || serialNumber || "";
            const identifier = parseCustomerIdentifierFromNfc(payload);
            const found = customers.find(
              (c) => c.id.toUpperCase() === identifier.toUpperCase() || c.phone === identifier
            );
            if (found) {
              selectedAdminCustomer = found;
              statusEl.textContent = `تمت قراءة بطاقة ${found.name}`;
              renderAdmin();
            } else {
              statusEl.textContent = `تمت قراءة (${identifier}) ولم يُعثر على زبون مطابق`;
            }
          });
        } catch (err) {
          nfcAbortController = null;
          statusEl.textContent = "تعذّر تشغيل NDEFReader: " + (err && err.message ? err.message : err);
        }
      };
    }
  }

  function applyLangAndTheme() {
    document.documentElement.lang = lang;
    document.documentElement.dir = lang === "ar" ? "rtl" : "ltr";
    document.documentElement.setAttribute("data-theme", dark ? "dark" : "light");
    document.getElementById("langToggleBtn").textContent = lang === "ar" ? "FR" : "AR";
    document.getElementById("themeToggleBtn").textContent = dark ? "☀️" : "🌙";
    document.getElementById("modeLabel").textContent = cfg.appsScriptUrl ? "متصل بـ Google Sheets" : "وضع تجريبي محلي (Demo)";
  }

  document.getElementById("langToggleBtn").onclick = () => {
    lang = lang === "ar" ? "fr" : "ar";
    localStorage.setItem("loyalty_lang", lang);
    applyLangAndTheme();
    renderCustomer();
  };
  document.getElementById("themeToggleBtn").onclick = () => {
    dark = !dark;
    localStorage.setItem("loyalty_dark", dark ? "1" : "0");
    applyLangAndTheme();
  };
  document.getElementById("navCustomerBtn").onclick = () => {
    document.getElementById("customerView").classList.remove("hidden");
    document.getElementById("adminView").classList.add("hidden");
    renderCustomer();
  };
  document.getElementById("navAdminBtn").onclick = () => {
    document.getElementById("customerView").classList.add("hidden");
    document.getElementById("adminView").classList.remove("hidden");
    renderAdmin();
  };

  window.addEventListener("beforeinstallprompt", (e) => {
    e.preventDefault();
    deferredPrompt = e;
    const btn = document.getElementById("installPwaBtn");
    btn.classList.remove("hidden");
    btn.onclick = () => deferredPrompt.prompt();
  });

  if ("serviceWorker" in navigator) {
    navigator.serviceWorker.register("./sw.js").catch(() => {});
  }

  applyThemeColors(cfg);
  applyLangAndTheme();
  renderCustomer();
})();
