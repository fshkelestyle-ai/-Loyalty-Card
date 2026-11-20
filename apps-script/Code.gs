/**
 * ============================================================================
 * نظام بطاقة الولاء القابل للتخصيص (White-label Loyalty Card) - Google Apps Script
 * ============================================================================
 * الواجهة الخلفية المجانية على Google Sheets لحساب العميل الخاص.
 * المالك يحتفظ بملكية 100% من البيانات في جدول بيانات Google Sheets الخاص به.
 */

const SHEET_NAMES = {
  CUSTOMERS: 'Customers',
  TRANSACTIONS: 'Transactions',
  REWARDS: 'Rewards',
  PROMOTIONS: 'Promotions',
  SETTINGS: 'Settings',
};

const CACHE_TTL_SECONDS = 120; // تقليل استدعاءات القراءة من Sheets
const DEFAULT_ADMIN_TOKEN = 'admin1234'; // يُنصح بتغييره فوراً عبر setAdminToken أو Script Properties

/**
 * دالة التهيئة الأولى: تنشئ الأوراق والعناوين والبيانات الأولية تلقائياً.
 * شغّل هذه الدالة مرة واحدة بعد لصق الكود، أو تُستدعى تلقائياً عند أول طلب.
 */
function setup() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const props = PropertiesService.getScriptProperties();

  if (!props.getProperty('ADMIN_TOKEN')) {
    props.setProperty('ADMIN_TOKEN', DEFAULT_ADMIN_TOKEN);
  }

  // 1. ورقة الزبناء
  ensureSheetWithHeaders_(ss, SHEET_NAMES.CUSTOMERS, [
    'id',
    'name',
    'phone',
    'email',
    'points',
    'registeredAt',
    'lastVisit',
    'notificationConsent',
    'notes',
  ]);

  // 2. ورقة العمليات
  ensureSheetWithHeaders_(ss, SHEET_NAMES.TRANSACTIONS, [
    'id',
    'date',
    'customerId',
    'customerName',
    'customerPhone',
    'type',
    'points',
    'billAmountMad',
    'note',
  ]);

  // 3. ورقة المكافآت
  const rewardsSheet = ensureSheetWithHeaders_(ss, SHEET_NAMES.REWARDS, [
    'id',
    'title',
    'description',
    'pointsRequired',
    'active',
  ]);
  if (rewardsSheet.getLastRow() <= 1) {
    rewardsSheet.appendRow(['RWD-1', 'مشروب مجاني', 'قهوة أو عصير طازج حسب اختيارك', 50, true]);
    rewardsSheet.appendRow(['RWD-2', 'خصم 25 درهم', 'خصم مباشر على فاتورتك القادمة', 100, true]);
    rewardsSheet.appendRow(['RWD-3', 'وجبة أو حلوى مجانية', 'هدية خاصة لزبنائنا الأوفياء', 200, true]);
  }

  // 4. ورقة العروض المرسلة
  ensureSheetWithHeaders_(ss, SHEET_NAMES.PROMOTIONS, [
    'id',
    'date',
    'title',
    'message',
    'channel',
    'audience',
    'recipientCount',
  ]);

  // 5. ورقة الإعدادات
  const settingsSheet = ensureSheetWithHeaders_(ss, SHEET_NAMES.SETTINGS, ['key', 'value']);
  if (settingsSheet.getLastRow() <= 1) {
    const defaults = [
      ['businessName', 'نشاطك التجاري'],
      ['businessType', 'مقهى ومطعم'],
      ['logoUrl', ''],
      ['primaryColor', '#0F766E'],
      ['secondaryColor', '#D97706'],
      ['backgroundColor', '#FAF7F2'],
      ['welcomeMessage', 'مرحباً بك في برنامج الولاء! اجمع النقاط مع كل زيارة واستبدلها بمكافآت حصرية.'],
      ['termsText', 'تُحتسب النقاط عند كل عملية شراء. المكافآت غير قابلة للتحويل النقدي.'],
      ['privacyText', 'نلتزم بحماية بياناتك الشخصية واستخدام رقم هاتفك وبريدك فقط لإدارة نقاطك وإرسال العروض بعد موافقتك الصريحة.'],
      ['pointsPerMad', '10'],
      ['currency', 'MAD'],
      ['customerPortalUrl', ''],
    ];
    defaults.forEach((row) => settingsSheet.appendRow(row));
  }

  invalidateCache_();
  return { ok: true, message: 'تمت تهيئة أوراق البيانات بنجاح' };
}

/**
 * دالة مساعدة لتعيين كلمة سر المدير بأمان في Script Properties
 */
function setAdminToken(newToken) {
  if (!newToken || String(newToken).trim().length < 4) {
    throw new Error('كلمة سر المدير يجب أن تتكون من 4 أحرف على الأقل');
  }
  PropertiesService.getScriptProperties().setProperty('ADMIN_TOKEN', String(newToken).trim());
  return 'تم تحديث كلمة سر المدير بنجاح';
}

/**
 * نقطة الدخول لطلبات GET
 */
function doGet(e) {
  try {
    ensureInitialized_();
    const params = (e && e.parameter) || {};
    const action = params.action || 'getPublicConfig';
    return jsonResponse_(routeAction_(action, params));
  } catch (err) {
    return jsonResponse_({
      ok: false,
      error: err.message || 'حدث خطأ غير متوقع في الخادم',
    });
  }
}

/**
 * نقطة الدخول لطلبات POST (تقبل JSON أو text/plain لتجنب مشاكل CORS Preflight)
 */
function doPost(e) {
  try {
    ensureInitialized_();
    let payload = {};
    if (e && e.postData && e.postData.contents) {
      payload = JSON.parse(e.postData.contents);
    } else if (e && e.parameter) {
      payload = e.parameter;
    }
    const action = payload.action || (e && e.parameter && e.parameter.action) || '';
    return jsonResponse_(routeAction_(action, payload));
  } catch (err) {
    return jsonResponse_({
      ok: false,
      error: err.message || 'تعذّر قراءة البيانات المرسلة',
    });
  }
}

/**
 * موجه العمليات الرئيسي
 */
function routeAction_(action, data) {
  switch (action) {
    // --- عمليات واجهة الزبون (عامة / محدودة ببيانات الزبون فقط) ---
    case 'ping':
      return { ok: true, message: 'الاتصال ناجح بقاعدة البيانات', timestamp: new Date().toISOString() };
    case 'getPublicConfig':
      return handleGetPublicConfig_();
    case 'registerCustomer':
      return handleRegisterCustomer_(data);
    case 'getCustomerCard':
      return handleGetCustomerCard_(data);

    // --- عمليات لوحة المدير (تتطلب كلمة سر المدير ADMIN_TOKEN) ---
    case 'verifyAdmin':
      requireAdmin_(data);
      return { ok: true, message: 'تم التحقق من صلاحية المدير بنجاح' };
    case 'getDashboard':
      requireAdmin_(data);
      return handleGetDashboard_();
    case 'listCustomers':
      requireAdmin_(data);
      return handleListCustomers_(data);
    case 'getCustomerDetails':
      requireAdmin_(data);
      return handleGetCustomerDetails_(data);
    case 'addPoints':
      requireAdmin_(data);
      return handleAddPoints_(data);
    case 'redeemPoints':
      requireAdmin_(data);
      return handleRedeemPoints_(data);
    case 'saveReward':
      requireAdmin_(data);
      return handleSaveReward_(data);
    case 'deleteReward':
      requireAdmin_(data);
      return handleDeleteReward_(data);
    case 'saveSettings':
      requireAdmin_(data);
      return handleSaveSettings_(data);
    case 'sendPromotion':
      requireAdmin_(data);
      return handleSendPromotion_(data);
    case 'getQuota':
      requireAdmin_(data);
      return { ok: true, remainingEmailQuota: MailApp.getRemainingDailyQuota() };
    default:
      return { ok: false, error: 'العملية المطلوبة غير معروفة: ' + action };
  }
}

/* ============================================================================
 * معالجات واجهة الزبون
 * ============================================================================ */

function handleGetPublicConfig_() {
  const cache = CacheService.getScriptCache();
  const cached = cache.get('PUBLIC_CONFIG');
  if (cached) {
    return JSON.parse(cached);
  }

  const settings = getSettingsMap_();
  const rewards = getSheetObjects_(SHEET_NAMES.REWARDS).filter((r) => toBool_(r.active));
  const promotions = getSheetObjects_(SHEET_NAMES.PROMOTIONS)
    .slice(-5)
    .reverse()
    .map((p) => ({
      id: p.id,
      date: p.date,
      title: p.title,
      message: p.message,
    }));

  const response = {
    ok: true,
    settings,
    rewards,
    promotions,
  };
  cache.put('PUBLIC_CONFIG', JSON.stringify(response), CACHE_TTL_SECONDS);
  return response;
}

function handleRegisterCustomer_(data) {
  const name = String(data.name || '').trim();
  const phone = normalizePhone_(data.phone);
  const email = String(data.email || '').trim();
  const consent = toBool_(data.notificationConsent);

  if (name.length < 2) {
    return { ok: false, error: 'المرجو إدخال الاسم الكامل بشكل صحيح' };
  }
  if (!phone || phone.length < 8) {
    return { ok: false, error: 'رقم الهاتف غير صالح. مثال: 0612345678' };
  }
  if (!consent) {
    return { ok: false, error: 'يجب الموافقة الصريحة على استقبال العروض وشروط الخصوصية' };
  }

  const lock = LockService.getScriptLock();
  lock.waitLock(10000);
  try {
    const ss = SpreadsheetApp.getActiveSpreadsheet();
    const sheet = ss.getSheetByName(SHEET_NAMES.CUSTOMERS);
    const customers = getSheetObjects_(SHEET_NAMES.CUSTOMERS);

    const existing = customers.find((c) => normalizePhone_(c.phone) === phone);
    if (existing) {
      return {
        ok: false,
        code: 'DUPLICATE_PHONE',
        error: 'رقم الهاتف مسجّل مسبقاً في نظام الولاء. يمكنك فتح بطاقتك مباشرة.',
        customer: sanitizeCustomer_(existing),
      };
    }

    const now = new Date().toISOString();
    const id = 'CUS-' + Utilities.getUuid().slice(0, 6).toUpperCase();
    sheet.appendRow([id, name, phone, email, 0, now, now, true, '']);
    invalidateCache_();

    const newCustomer = {
      id,
      name,
      phone,
      email,
      points: 0,
      registeredAt: now,
      lastVisit: now,
      notificationConsent: true,
    };

    return {
      ok: true,
      message: 'تم تسجيلك بنجاح! مرحباً بك في بطاقة الولاء',
      customer: newCustomer,
    };
  } finally {
    lock.releaseLock();
  }
}

function handleGetCustomerCard_(data) {
  const phone = normalizePhone_(data.phone);
  const id = String(data.id || '').trim();
  if (!phone && !id) {
    return { ok: false, error: 'المرجو إدخال رقم الهاتف لعرض بطاقتك' };
  }

  const customers = getSheetObjects_(SHEET_NAMES.CUSTOMERS);
  const customer = customers.find((c) => (id && c.id === id) || (phone && normalizePhone_(c.phone) === phone));
  if (!customer) {
    return { ok: false, error: 'لم يتم العثور على بطاقة مسجّلة بهذا الرقم' };
  }

  // إرجاع معاملات هذا الزبون فقط لحماية الخصوصية
  const transactions = getSheetObjects_(SHEET_NAMES.TRANSACTIONS)
    .filter((t) => t.customerId === customer.id || normalizePhone_(t.customerPhone) === normalizePhone_(customer.phone))
    .reverse()
    .slice(0, 30);

  const pub = handleGetPublicConfig_();
  return {
    ok: true,
    customer: sanitizeCustomer_(customer),
    transactions,
    rewards: pub.rewards,
    promotions: pub.promotions,
    settings: pub.settings,
  };
}

/* ============================================================================
 * معالجات لوحة المدير
 * ============================================================================ */

function handleGetDashboard_() {
  const customers = getSheetObjects_(SHEET_NAMES.CUSTOMERS);
  const transactions = getSheetObjects_(SHEET_NAMES.TRANSACTIONS);
  const rewards = getSheetObjects_(SHEET_NAMES.REWARDS);
  const promotions = getSheetObjects_(SHEET_NAMES.PROMOTIONS);
  const settings = getSettingsMap_();

  const totalPointsDistributed = transactions
    .filter((t) => t.type === 'إضافة')
    .reduce((sum, t) => sum + Number(t.points || 0), 0);

  const topCustomers = customers
    .slice()
    .sort((a, b) => Number(b.points || 0) - Number(a.points || 0))
    .slice(0, 5)
    .map(sanitizeCustomer_);

  return {
    ok: true,
    stats: {
      totalCustomers: customers.length,
      totalPointsDistributed,
      activeRewardsCount: rewards.filter((r) => toBool_(r.active)).length,
      remainingEmailQuota: MailApp.getRemainingDailyQuota(),
    },
    topCustomers,
    recentTransactions: transactions.slice(-20).reverse(),
    rewards,
    promotions: promotions.slice(-20).reverse(),
    settings,
  };
}

function handleListCustomers_(data) {
  const query = String(data.query || '').trim().toLowerCase();
  let customers = getSheetObjects_(SHEET_NAMES.CUSTOMERS).map(sanitizeCustomer_);
  if (query) {
    customers = customers.filter(
      (c) =>
        c.name.toLowerCase().includes(query) ||
        c.phone.toLowerCase().includes(query) ||
        c.email.toLowerCase().includes(query)
    );
  }
  customers.sort((a, b) => Number(b.points) - Number(a.points));
  return { ok: true, customers };
}

function handleGetCustomerDetails_(data) {
  const id = String(data.customerId || data.id || '').trim();
  const phone = normalizePhone_(data.phone);
  const customers = getSheetObjects_(SHEET_NAMES.CUSTOMERS);
  const customer = customers.find((c) => (id && c.id === id) || (phone && normalizePhone_(c.phone) === phone));
  if (!customer) {
    return { ok: false, error: 'الزبون غير موجود' };
  }
  const transactions = getSheetObjects_(SHEET_NAMES.TRANSACTIONS)
    .filter((t) => t.customerId === customer.id)
    .reverse();
  return { ok: true, customer: sanitizeCustomer_(customer), transactions };
}

function handleAddPoints_(data) {
  const lock = LockService.getScriptLock();
  lock.waitLock(10000);
  try {
    const ss = SpreadsheetApp.getActiveSpreadsheet();
    const sheet = ss.getSheetByName(SHEET_NAMES.CUSTOMERS);
    const txSheet = ss.getSheetByName(SHEET_NAMES.TRANSACTIONS);
    const settings = getSettingsMap_();
    const rate = Math.max(1, Number(settings.pointsPerMad || 10));

    const billAmount = Number(data.billAmountMad || 0);
    let pointsToAdd = Number(data.points || 0);
    if (billAmount > 0 && pointsToAdd <= 0) {
      pointsToAdd = Math.floor(billAmount / rate);
    }
    if (pointsToAdd <= 0) {
      return { ok: false, error: 'عدد النقاط يجب أن يكون أكبر من صفر (أو أدخل مبلغ فاتورة كافياً)' };
    }

    const rowInfo = findCustomerRow_(sheet, data.customerId, data.phone);
    if (!rowInfo) {
      return { ok: false, error: 'لم يتم العثور على الزبون' };
    }

    const currentPoints = Number(rowInfo.values[4] || 0);
    const updatedPoints = currentPoints + pointsToAdd;
    const now = new Date().toISOString();

    sheet.getRange(rowInfo.rowIndex, 5).setValue(updatedPoints);
    sheet.getRange(rowInfo.rowIndex, 7).setValue(now);

    const txId = 'TX-' + Utilities.getUuid().slice(0, 6).toUpperCase();
    const note = String(data.note || (billAmount > 0 ? 'فاتورة بقيمة ' + billAmount + ' درهم' : 'إضافة يدوية'));
    txSheet.appendRow([
      txId,
      now,
      rowInfo.values[0],
      rowInfo.values[1],
      rowInfo.values[2],
      'إضافة',
      pointsToAdd,
      billAmount,
      note,
    ]);

    invalidateCache_();
    return {
      ok: true,
      message: 'تمت إضافة ' + pointsToAdd + ' نقطة بنجاح',
      newPoints: updatedPoints,
    };
  } finally {
    lock.releaseLock();
  }
}

function handleRedeemPoints_(data) {
  const lock = LockService.getScriptLock();
  lock.waitLock(10000);
  try {
    const ss = SpreadsheetApp.getActiveSpreadsheet();
    const sheet = ss.getSheetByName(SHEET_NAMES.CUSTOMERS);
    const txSheet = ss.getSheetByName(SHEET_NAMES.TRANSACTIONS);

    const pointsToRedeem = Number(data.points || 0);
    if (pointsToRedeem <= 0) {
      return { ok: false, error: 'عدد النقاط المطلوب خصمها غير صالح' };
    }

    const rowInfo = findCustomerRow_(sheet, data.customerId, data.phone);
    if (!rowInfo) {
      return { ok: false, error: 'لم يتم العثور على الزبون' };
    }

    const currentPoints = Number(rowInfo.values[4] || 0);
    if (currentPoints < pointsToRedeem) {
      return {
        ok: false,
        error: 'رصيد الزبون (' + currentPoints + ' نقطة) غير كافٍ لاستبدال هذه المكافأة (' + pointsToRedeem + ' نقطة)',
      };
    }

    const updatedPoints = currentPoints - pointsToRedeem;
    const now = new Date().toISOString();

    sheet.getRange(rowInfo.rowIndex, 5).setValue(updatedPoints);
    sheet.getRange(rowInfo.rowIndex, 7).setValue(now);

    const txId = 'TX-' + Utilities.getUuid().slice(0, 6).toUpperCase();
    const note = String(data.rewardTitle || data.note || 'استبدال مكافأة');
    txSheet.appendRow([
      txId,
      now,
      rowInfo.values[0],
      rowInfo.values[1],
      rowInfo.values[2],
      'استبدال',
      pointsToRedeem,
      0,
      note,
    ]);

    invalidateCache_();
    return {
      ok: true,
      message: 'تم استبدال المكافأة وخصم ' + pointsToRedeem + ' نقطة بنجاح',
      newPoints: updatedPoints,
    };
  } finally {
    lock.releaseLock();
  }
}

function handleSaveReward_(data) {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const sheet = ss.getSheetByName(SHEET_NAMES.REWARDS);
  const title = String(data.title || '').trim();
  const description = String(data.description || '').trim();
  const pointsRequired = Number(data.pointsRequired || 0);
  const active = data.active === undefined ? true : toBool_(data.active);

  if (!title || pointsRequired <= 0) {
    return { ok: false, error: 'المرجو إدخال اسم المكافأة وعدد نقاط صالح' };
  }

  const values = sheet.getDataRange().getValues();
  const id = String(data.id || '').trim();
  if (id) {
    for (let i = 1; i < values.length; i++) {
      if (values[i][0] === id) {
        sheet.getRange(i + 1, 2, 1, 4).setValues([[title, description, pointsRequired, active]]);
        invalidateCache_();
        return { ok: true, message: 'تم تحديث المكافأة بنجاح' };
      }
    }
  }

  const newId = 'RWD-' + Utilities.getUuid().slice(0, 5).toUpperCase();
  sheet.appendRow([newId, title, description, pointsRequired, active]);
  invalidateCache_();
  return { ok: true, message: 'تمت إضافة المكافأة بنجاح', id: newId };
}

function handleDeleteReward_(data) {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const sheet = ss.getSheetByName(SHEET_NAMES.REWARDS);
  const id = String(data.id || '').trim();
  const values = sheet.getDataRange().getValues();
  for (let i = 1; i < values.length; i++) {
    if (values[i][0] === id) {
      sheet.deleteRow(i + 1);
      invalidateCache_();
      return { ok: true, message: 'تم حذف المكافأة' };
    }
  }
  return { ok: false, error: 'المكافأة غير موجودة' };
}

function handleSaveSettings_(data) {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const sheet = ss.getSheetByName(SHEET_NAMES.SETTINGS);
  const updates = data.settings || data;
  const allowedKeys = [
    'businessName',
    'businessType',
    'logoUrl',
    'primaryColor',
    'secondaryColor',
    'backgroundColor',
    'welcomeMessage',
    'termsText',
    'privacyText',
    'pointsPerMad',
    'currency',
    'customerPortalUrl',
  ];

  const rows = sheet.getDataRange().getValues();
  const indexByKey = {};
  for (let i = 1; i < rows.length; i++) {
    indexByKey[rows[i][0]] = i + 1;
  }

  allowedKeys.forEach((key) => {
    if (updates[key] !== undefined) {
      const val = String(updates[key]);
      if (indexByKey[key]) {
        sheet.getRange(indexByKey[key], 2).setValue(val);
      } else {
        sheet.appendRow([key, val]);
      }
    }
  });

  invalidateCache_();
  return { ok: true, message: 'تم حفظ إعدادات الهوية والولاء بنجاح', settings: getSettingsMap_() };
}

function handleSendPromotion_(data) {
  const title = String(data.title || '').trim();
  const message = String(data.message || '').trim();
  const channel = String(data.channel || 'email'); // email | whatsapp | both
  const audience = String(data.audience || 'all'); // all | vip100 | active30
  if (!title || !message) {
    return { ok: false, error: 'المرجو إدخال عنوان العرض ونص الرسالة' };
  }

  const settings = getSettingsMap_();
  const businessName = settings.businessName || 'بطاقة الولاء';
  const customers = getSheetObjects_(SHEET_NAMES.CUSTOMERS).filter((c) => toBool_(c.notificationConsent));

  let targets = customers;
  if (audience === 'vip100') {
    targets = customers.filter((c) => Number(c.points || 0) >= 100);
  } else if (audience === 'active30') {
    const cutoff = Date.now() - 30 * 24 * 60 * 60 * 1000;
    targets = customers.filter((c) => new Date(c.lastVisit).getTime() >= cutoff);
  }

  let emailsSent = 0;
  let remainingQuota = MailApp.getRemainingDailyQuota();
  const whatsappLinks = [];

  targets.forEach((c) => {
    if ((channel === 'email' || channel === 'both') && c.email && remainingQuota > 0) {
      try {
        MailApp.sendEmail({
          to: c.email,
          subject: businessName + ' - ' + title,
          body: 'مرحباً ' + c.name + '،\n\n' + message + '\n\nرصيدك الحالي: ' + c.points + ' نقطة.\n\nمع تحيات ' + businessName,
        });
        emailsSent++;
        remainingQuota--;
      } catch (mailErr) {
        // متابعة الإرسال لبقية الزبناء في حال تعذر بريد واحد
      }
    }
    if (channel === 'whatsapp' || channel === 'both') {
      const waPhone = toWhatsappNumber_(c.phone);
      const text = encodeURIComponent('مرحباً ' + c.name + ' 👋\n*' + businessName + '*\n' + title + '\n' + message + '\nرصيدك الحالي: ' + c.points + ' نقطة');
      whatsappLinks.push({
        customerId: c.id,
        name: c.name,
        phone: c.phone,
        url: 'https://wa.me/' + waPhone + '?text=' + text,
      });
    }
  });

  const promoSheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(SHEET_NAMES.PROMOTIONS);
  const promoId = 'PRM-' + Utilities.getUuid().slice(0, 6).toUpperCase();
  promoSheet.appendRow([promoId, new Date().toISOString(), title, message, channel, audience, targets.length]);
  invalidateCache_();

  return {
    ok: true,
    message: 'تم تسجيل الحملة الترويجية وإرسال ' + emailsSent + ' رسالة بريدية',
    targetCount: targets.length,
    emailsSent,
    remainingEmailQuota: MailApp.getRemainingDailyQuota(),
    whatsappLinks,
  };
}

/* ============================================================================
 * أدوات مساعدة
 * ============================================================================ */

function ensureInitialized_() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  if (!ss.getSheetByName(SHEET_NAMES.CUSTOMERS) || !ss.getSheetByName(SHEET_NAMES.SETTINGS)) {
    setup();
  }
}

function ensureSheetWithHeaders_(ss, name, headers) {
  let sheet = ss.getSheetByName(name);
  if (!sheet) {
    sheet = ss.insertSheet(name);
  }
  if (sheet.getLastRow() === 0) {
    sheet.appendRow(headers);
    sheet.getRange(1, 1, 1, headers.length).setFontWeight('bold');
    sheet.setFrozenRows(1);
  }
  return sheet;
}

function getSheetObjects_(sheetName) {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const sheet = ss.getSheetByName(sheetName);
  if (!sheet || sheet.getLastRow() < 2) return [];
  const data = sheet.getDataRange().getValues();
  const headers = data[0];
  const results = [];
  for (let i = 1; i < data.length; i++) {
    const obj = {};
    for (let j = 0; j < headers.length; j++) {
      obj[headers[j]] = data[i][j];
    }
    results.push(obj);
  }
  return results;
}

function getSettingsMap_() {
  const rows = getSheetObjects_(SHEET_NAMES.SETTINGS);
  const map = {};
  rows.forEach((r) => {
    if (r.key) map[r.key] = r.value;
  });
  return map;
}

function findCustomerRow_(sheet, customerId, phone) {
  const values = sheet.getDataRange().getValues();
  const targetPhone = normalizePhone_(phone);
  for (let i = 1; i < values.length; i++) {
    if ((customerId && values[i][0] === customerId) || (targetPhone && normalizePhone_(values[i][2]) === targetPhone)) {
      return { rowIndex: i + 1, values: values[i] };
    }
  }
  return null;
}

function sanitizeCustomer_(c) {
  return {
    id: String(c.id || ''),
    name: String(c.name || ''),
    phone: String(c.phone || ''),
    email: String(c.email || ''),
    points: Number(c.points || 0),
    registeredAt: String(c.registeredAt || ''),
    lastVisit: String(c.lastVisit || ''),
    notificationConsent: toBool_(c.notificationConsent),
  };
}

function requireAdmin_(data) {
  const expected = PropertiesService.getScriptProperties().getProperty('ADMIN_TOKEN') || DEFAULT_ADMIN_TOKEN;
  const provided = String((data && (data.token || data.adminToken)) || '').trim();
  if (!provided || provided !== expected) {
    throw new Error('كلمة سر المدير غير صحيحة أو مفقودة');
  }
}

function normalizePhone_(phone) {
  let digits = String(phone || '').replace(/[^\d+]/g, '');
  if (digits.startsWith('+212')) digits = '0' + digits.slice(4);
  if (digits.startsWith('212') && digits.length >= 12) digits = '0' + digits.slice(3);
  return digits;
}

function toWhatsappNumber_(phone) {
  const normalized = normalizePhone_(phone);
  if (normalized.startsWith('0')) return '212' + normalized.slice(1);
  return normalized.replace(/^\+/, '');
}

function toBool_(val) {
  return val === true || String(val).toLowerCase() === 'true' || val === 1 || val === '1';
}

function invalidateCache_() {
  CacheService.getScriptCache().remove('PUBLIC_CONFIG');
}

function jsonResponse_(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}
