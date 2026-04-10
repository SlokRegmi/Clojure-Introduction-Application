(ns my-playground.frontend
  (:require [clojure.string :as str]
            [hiccup.page :refer [html5]]))

;; ── Colour palette ────────────────────────────────────────────────────────────
;; Primary  #1d4ed8  (royal blue)
;; Accent   #0ea5e9  (sky)
;; Success  #059669  (emerald)
;; Warning  #d97706  (amber)
;; Danger   #dc2626  (red)
;; Bg       #f0f4f8  (cool gray)

(def ^:private common-styles
  "
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { font-family: 'Segoe UI', system-ui, sans-serif; background: #f0f4f8; color: #1e293b; }

  /* Header */
  header {
    background: linear-gradient(135deg, #0f172a 0%, #1d4ed8 100%);
    color: #fff;
    padding: 1rem 1.6rem;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 2px 8px rgba(15,23,42,.25);
  }
  header h1 { font-size: 1.2rem; font-weight: 700; letter-spacing: .03em; }
  .header-right { display: flex; align-items: center; gap: .6rem; }

  /* Badges */
  .badge-stats {
    background: rgba(255,255,255,.12);
    border: 1px solid rgba(255,255,255,.2);
    border-radius: 999px;
    padding: .25rem .7rem;
    font-size: .76rem;
  }
  .badge-role {
    border-radius: 999px;
    padding: .25rem .7rem;
    font-size: .76rem;
    font-weight: 700;
    text-transform: capitalize;
  }
  .badge-role.role-admin  { background: #fbbf24; color: #78350f; }
  .badge-role.role-editor { background: #34d399; color: #064e3b; }
  .badge-role.role-viewer { background: #94a3b8; color: #1e293b; }

  /* Layout */
  main { max-width: 1200px; margin: 1.4rem auto; padding: 0 .9rem 2.5rem; }
  .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(350px, 1fr)); gap: 1.1rem; }

  /* Cards */
  .card {
    background: #fff;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    padding: 1.1rem 1.2rem;
    box-shadow: 0 2px 8px rgba(15,23,42,.05);
  }
  .card h2 {
    font-size: .82rem;
    color: #64748b;
    margin-bottom: .9rem;
    text-transform: uppercase;
    letter-spacing: .08em;
    font-weight: 700;
    display: flex;
    align-items: center;
    gap: .4rem;
  }
  .card h2::before {
    content: '';
    display: inline-block;
    width: 3px;
    height: 13px;
    background: #1d4ed8;
    border-radius: 2px;
  }

  /* Read-only notice */
  .readonly-notice {
    background: #f1f5f9;
    border: 1px solid #cbd5e1;
    border-radius: 8px;
    padding: .5rem .75rem;
    font-size: .8rem;
    color: #64748b;
    margin-bottom: .75rem;
    display: flex;
    align-items: center;
    gap: .4rem;
  }

  /* Forms */
  .form-row { display: flex; gap: .45rem; flex-wrap: wrap; margin-bottom: .55rem; }
  input, select {
    border: 1px solid #cbd5e1;
    border-radius: 8px;
    padding: .48rem .65rem;
    min-height: 36px;
    font-size: .875rem;
    flex: 1;
    min-width: 110px;
    background: #f8fafc;
    transition: border-color .15s, box-shadow .15s;
  }
  input:focus, select:focus {
    outline: none;
    border-color: #1d4ed8;
    background: #fff;
    box-shadow: 0 0 0 3px rgba(29,78,216,.12);
  }

  /* Buttons */
  button {
    border: none;
    border-radius: 8px;
    padding: .48rem .85rem;
    font-size: .85rem;
    font-weight: 600;
    cursor: pointer;
    transition: filter .15s, opacity .15s;
  }
  button:hover { filter: brightness(1.08); }
  .btn-primary { background: #1d4ed8; color: #fff; }
  .btn-success { background: #059669; color: #fff; }
  .btn-warning { background: #d97706; color: #fff; }
  .btn-danger  { background: #dc2626; color: #fff; }
  .btn-muted   { background: #e2e8f0; color: #475569; }
  .btn-logout  {
    background: rgba(255,255,255,.12);
    color: #fff;
    border: 1px solid rgba(255,255,255,.25);
    font-size: .78rem;
    padding: .28rem .65rem;
  }
  .btn-logout:hover { background: rgba(255,255,255,.22); filter: none; }

  /* Tables */
  .table-wrap { overflow-x: auto; }
  table { width: 100%; border-collapse: collapse; }
  th {
    text-align: left;
    font-size: .71rem;
    color: #64748b;
    text-transform: uppercase;
    letter-spacing: .06em;
    padding: .4rem .45rem;
    border-bottom: 2px solid #e2e8f0;
  }
  td { font-size: .85rem; padding: .5rem .45rem; border-bottom: 1px solid #f1f5f9; vertical-align: middle; }
  tr:last-child td { border-bottom: none; }
  tr:hover td { background: #f8fafc; }

  /* Pills */
  .pill { border-radius: 999px; padding: .15rem .55rem; font-size: .71rem; font-weight: 700; }
  .pill-pending  { background: #fef3c7; color: #92400e; }
  .pill-active   { background: #d1fae5; color: #065f46; }
  .pill-closed   { background: #f1f5f9; color: #64748b; }
  .pill-checking { background: #dbeafe; color: #1e40af; }
  .pill-savings  { background: #ede9fe; color: #4c1d95; }

  /* Notification */
  #msg { margin: .9rem 0 0; font-size: .86rem; min-height: 1.1em; padding: .4rem .6rem; border-radius: 7px; }
  #msg:not(:empty) { background: #f0fdf4; border: 1px solid #bbf7d0; color: #15803d; }
  #msg.is-error   { background: #fef2f2; border: 1px solid #fecaca; color: #dc2626; }

  /* Actions */
  .actions { display: flex; gap: .3rem; flex-wrap: wrap; }

  /* Divider inside card */
  .card-divider { border: none; border-top: 1px solid #e2e8f0; margin: .9rem 0 .75rem; }

  @media (max-width: 760px) {
    header { flex-direction: column; align-items: flex-start; gap: .4rem; }
    .grid { grid-template-columns: 1fr; }
  }
  ")

(def ^:private login-extra-styles
  "
  body { display: flex; flex-direction: column; min-height: 100vh; }
  .login-wrap { flex: 1; display: flex; align-items: center; justify-content: center; padding: 2rem; }
  .login-card {
    background: #fff;
    border: 1px solid #e2e8f0;
    border-radius: 14px;
    padding: 2.2rem 2.4rem;
    width: 100%;
    max-width: 390px;
    box-shadow: 0 12px 32px rgba(15,23,42,.10);
  }
  .login-logo { font-size: 1.4rem; font-weight: 800; color: #1d4ed8; margin-bottom: .3rem; }
  .login-sub  { font-size: .83rem; color: #94a3b8; margin-bottom: 1.6rem; }
  .login-card label {
    display: block;
    font-size: .78rem;
    color: #64748b;
    margin-bottom: .3rem;
    margin-top: 1rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: .05em;
  }
  .login-card input { width: 100%; min-width: unset; flex: unset; }
  .login-card .btn-primary { width: 100%; margin-top: 1.4rem; padding: .7rem; font-size: .95rem; }
  .error-msg {
    background: #fef2f2;
    border: 1px solid #fecaca;
    border-radius: 8px;
    color: #dc2626;
    font-size: .84rem;
    padding: .55rem .75rem;
    margin-top: .9rem;
  }
  .hint {
    font-size: .76rem;
    color: #94a3b8;
    margin-top: 1.3rem;
    line-height: 1.6;
    border-top: 1px solid #f1f5f9;
    padding-top: .9rem;
  }
  .hint strong { color: #475569; }
  .hint .role-tag {
    display: inline-block;
    border-radius: 999px;
    padding: .05rem .4rem;
    font-size: .7rem;
    font-weight: 700;
    vertical-align: middle;
  }
  .hint .r-admin  { background: #fef3c7; color: #92400e; }
  .hint .r-editor { background: #d1fae5; color: #065f46; }
  .hint .r-viewer { background: #f1f5f9;  color: #475569; }
  ")

(defn login-page [error-msg]
  (html5
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
    [:title "PocketBank — Sign In"]
    [:style (str common-styles login-extra-styles)]]
   [:body
    [:header
     [:h1 "PocketBank"]]
    [:div.login-wrap
     [:div.login-card
      [:div.login-logo "PocketBank"]
      [:div.login-sub "Operations Console"]
      [:form {:method "POST" :action "/login"}
       [:label {:for "email"} "Email address"]
       [:input {:type "email" :id "email" :name "email"
                :placeholder "you@example.com" :required true :autofocus true}]
       [:label {:for "password"} "Password"]
       [:input {:type "password" :id "password" :name "password"
                :placeholder "••••••••" :required true}]
       (when error-msg
         [:p.error-msg error-msg])
       [:button.btn-primary {:type "submit"} "Sign In"]]
      [:div.hint
       [:strong "Demo accounts"] [:br]
       "alice@example.com / admin123 "
       [:span.role-tag.r-admin "Admin"] [:br]
       "bob@example.com / editor123 "
       [:span.role-tag.r-editor "Editor"] [:br]
       "carol@example.com / viewer123 "
       [:span.role-tag.r-viewer "Viewer"]]]]]))

(defn page [current-user]
  (let [user-name  (:name current-user)
        user-role  (:role current-user)
        user-id    (:id current-user)
        admin?     (= user-role "admin")
        editor?    (contains? #{"admin" "editor"} user-role)
        viewer?    (= user-role "viewer")]
    (html5
     [:head
      [:meta {:charset "UTF-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
      [:title "PocketBank Console"]
      [:style common-styles]]
     [:body
      [:header
       [:h1 "PocketBank Operations Console"]
       [:div.header-right
        [:span#stats.badge-stats "Loading…"]
        [:span {:class (str "badge-role role-" user-role)}
         user-name " · " (str/capitalize user-role)]
        [:form {:method "POST" :action "/logout" :style "margin:0"}
         [:button.btn-logout {:type "submit"} "Sign Out"]]]]

      [:main
       [:div.grid

        ;; ── User Management (admin only) ────────────────────────────────────
        (when admin?
          [:div.card
           [:h2 "User Management"]
           [:div.form-row
            [:input#inp-name  {:type "text"  :placeholder "Full name"}]
            [:input#inp-email {:type "email" :placeholder "Email address"}]
            [:select#inp-role
             [:option {:value "viewer"} "Viewer"]
             [:option {:value "editor"} "Editor"]
             [:option {:value "admin"}  "Admin"]]
            [:button.btn-primary {:onclick "addUser()"} "Add User"]]
           [:div.table-wrap
            [:table
             [:thead [:tr [:th "Name"] [:th "Email"] [:th "Role"] [:th ""]]]
             [:tbody#user-tbody [:tr [:td {:colspan "4"} "Loading users…"]]]]]])

        ;; ── Accounts ────────────────────────────────────────────────────────
        [:div.card
         [:h2 "Accounts"]
         (when editor?
           [:div
            [:div.form-row
             [:select#create-user-id]
             [:select#create-account-type
              [:option {:value "checking"} "Checking"]
              [:option {:value "savings"}  "Savings"]]
             [:input#create-initial-balance
              {:type "number" :step "0.01" :placeholder "Initial balance"}]
             [:button.btn-primary {:onclick "createAccount()"} "Open Account"]]
            [:hr.card-divider]])
         (when viewer?
           [:div.readonly-notice "Read-only — contact an editor to create accounts"])
         [:div.table-wrap
          [:table
           [:thead [:tr [:th "ID"] [:th "Owner"] [:th "Type"] [:th "Balance"] [:th "Status"]]]
           [:tbody#account-tbody [:tr [:td {:colspan "5"} "Loading accounts…"]]]]]]

        ;; ── Deposit / Withdraw (editor+ only) ───────────────────────────────
        (when editor?
          [:div.card
           [:h2 "Deposit & Withdraw"]
           [:div.form-row
            [:input#cash-account-id {:type "number" :placeholder "Account ID"}]
            [:input#cash-amount     {:type "number" :step "0.01" :placeholder "Amount"}]
            [:input#cash-desc       {:type "text"   :placeholder "Description (optional)"}]]
           [:div.form-row
            [:button.btn-success {:onclick "runCashAction('deposit')"}  "Deposit"]
            [:button.btn-warning {:onclick "runCashAction('withdraw')"} "Withdraw"]]])

        ;; ── Transfers ───────────────────────────────────────────────────────
        [:div.card
         [:h2 "Transfers"]
         (when editor?
           [:div
            [:div.form-row
             [:input#tr-from   {:type "number" :placeholder "From account ID"}]
             [:input#tr-to     {:type "number" :placeholder "To account ID"}]
             [:input#tr-amount {:type "number" :step "0.01" :placeholder "Amount"}]]
            [:div.form-row
             [:select#tr-mode
              [:option {:value "instant"} "Instant"]
              [:option {:value "request"} "Request approval"]]
             [:input#tr-desc {:type "text" :placeholder "Description (optional)"}]
             [:button.btn-primary {:onclick "createTransfer()"} "Submit Transfer"]]
            [:hr.card-divider]])
         (when viewer?
           [:div.readonly-notice "Read-only — you cannot initiate transfers"])
         [:div {:style "display:flex; justify-content:space-between; align-items:center; margin-bottom:.5rem;"}
          [:span {:style "font-size:.8rem; font-weight:700; color:#64748b; text-transform:uppercase; letter-spacing:.06em;"} "Pending Requests"]
          [:button.btn-muted {:onclick "loadPending()" :style "font-size:.78rem; padding:.3rem .6rem;"} "Refresh"]]
         [:div.table-wrap
          [:table
           [:thead [:tr [:th "ID"] [:th "From"] [:th "To"] [:th "Amount"] [:th "Status"] (when editor? [:th ""])]]
           [:tbody#pending-tbody [:tr [:td {:colspan (if editor? "6" "5")} "No pending requests."]]]]]]

        ;; ── Account History (all roles) ─────────────────────────────────────
        [:div.card
         [:h2 "Account History"]
         [:div.form-row
          [:input#history-account-id {:type "number" :placeholder "Account ID"}]
          [:button.btn-muted {:onclick "loadHistory()"} "Load History"]]
         [:div.table-wrap
          [:table
           [:thead [:tr [:th "Time"] [:th "Type"] [:th "Amount"] [:th "Balance After"] [:th "Description"]]]
           [:tbody#history-tbody [:tr [:td {:colspan "5"} "Enter an account ID to view history."]]]]]]]]

      [:div#msg ""]

      [:script (str "
const usersApi    = '/api/users';
const accountsApi = '/api/accounts';
const txApi       = '/api/transactions';
const currentUserId   = " user-id ";
const currentUserRole = '" user-role "';
const canWrite = " (if editor? "true" "false") ";
let users = [];

function esc(v) {
  return String(v == null ? '' : v)
    .replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/'/g,'&#39;');
}
function money(v) {
  const n = Number(v || 0);
  return Number.isFinite(n) ? n.toFixed(2) : '0.00';
}
function showMsg(text, isError) {
  const el = document.getElementById('msg');
  el.textContent = text;
  el.className = isError ? 'is-error' : '';
  clearTimeout(el._t);
  el._t = setTimeout(() => { if (el.textContent === text) { el.textContent = ''; el.className = ''; } }, 4000);
}
async function api(url, options) {
  const opts = options || {};
  const res = await fetch(url, Object.assign({}, opts, {
    headers: Object.assign({'Content-Type':'application/json'}, opts.headers || {})
  }));
  const text = await res.text();
  let payload = {};
  try { payload = text ? JSON.parse(text) : {}; } catch(_) { payload = {raw: text}; }
  if (!res.ok) throw new Error(payload.error || 'Request failed (' + res.status + ')');
  return payload;
}
function refreshStats(uc, ac) {
  document.getElementById('stats').textContent = uc + ' users | ' + ac + ' accounts';
}
function fillUserSelects() {
  const opts = users.map(u => `<option value='${u.id}'>${esc(u.name)} (#${u.id})</option>`).join('');
  const s = document.getElementById('create-user-id');
  if (s) s.innerHTML = opts || `<option value=''>No users</option>`;
}
async function loadUsers() {
  users = await api(usersApi);
  const tbody = document.getElementById('user-tbody');
  if (tbody) {
    tbody.innerHTML = users.length
      ? users.map(u => `<tr>
          <td>${esc(u.name)}</td>
          <td style='color:#64748b'>${esc(u.email)}</td>
          <td><span class='pill ${u.role==='admin'?'pill-admin':u.role==='editor'?'pill-editor':'pill-viewer'}'>${esc(u.role)}</span></td>
          <td><button class='btn-danger' style='font-size:.76rem;padding:.25rem .55rem' onclick='deleteUser(${u.id})'>Remove</button></td>
        </tr>`).join('')
      : `<tr><td colspan='4' style='color:#94a3b8'>No users found.</td></tr>`;
  }
  fillUserSelects();
}
async function addUser() {
  const name  = document.getElementById('inp-name').value.trim();
  const email = document.getElementById('inp-email').value.trim();
  const role  = document.getElementById('inp-role').value;
  if (!name || !email) { showMsg('Name and email are required', true); return; }
  try {
    await api(usersApi, {method:'POST', body:JSON.stringify({name, email, role})});
    document.getElementById('inp-name').value = '';
    document.getElementById('inp-email').value = '';
    await loadUsers(); await loadAccounts();
    showMsg('User added successfully', false);
  } catch(e) { showMsg(e.message, true); }
}
async function deleteUser(id) {
  if (!confirm('Remove this user? This cannot be undone.')) return;
  try {
    await api(usersApi + '/' + id, {method:'DELETE'});
    await loadUsers(); await loadAccounts();
    showMsg('User removed', false);
  } catch(e) { showMsg(e.message, true); }
}
async function loadAccounts() {
  const list = await api(accountsApi);
  const tbody = document.getElementById('account-tbody');
  if (!list.length) {
    tbody.innerHTML = `<tr><td colspan='5' style='color:#94a3b8'>No accounts found.</td></tr>`;
  } else {
    const ownerMap = {};
    users.forEach(u => { ownerMap[u.id] = u.name; });
    tbody.innerHTML = list.map(a => `<tr>
      <td style='color:#64748b;font-size:.8rem'>#${a.id}</td>
      <td>${esc(ownerMap[a['user-id']] || 'User #' + a['user-id'])}</td>
      <td><span class='pill ${a['account-type']==='checking'?'pill-checking':'pill-savings'}'>${esc(a['account-type'])}</span></td>
      <td style='font-variant-numeric:tabular-nums'>$${money(a.balance)}</td>
      <td><span class='pill ${a.status==='active'?'pill-active':'pill-closed'}'>${esc(a.status)}</span></td>
    </tr>`).join('');
  }
  refreshStats(users.length, list.length);
}
async function createAccount() {
  const userId = Number(document.getElementById('create-user-id').value);
  const type   = document.getElementById('create-account-type').value;
  const bal    = document.getElementById('create-initial-balance').value;
  try {
    await api(accountsApi, {method:'POST', body:JSON.stringify({'user-id':userId,'account-type':type,'initial-balance':bal===''?0:bal})});
    document.getElementById('create-initial-balance').value = '';
    await loadAccounts();
    showMsg('Account opened', false);
  } catch(e) { showMsg(e.message, true); }
}
async function runCashAction(kind) {
  const accountId = Number(document.getElementById('cash-account-id').value);
  const amount    = document.getElementById('cash-amount').value;
  const desc      = document.getElementById('cash-desc').value.trim();
  if (!accountId || !amount) { showMsg('Account ID and amount are required', true); return; }
  try {
    await api(txApi + '/' + kind, {method:'POST', body:JSON.stringify({'account-id':accountId, amount, description:desc})});
    await loadAccounts();
    showMsg(kind === 'deposit' ? 'Deposit completed' : 'Withdrawal completed', false);
  } catch(e) { showMsg(e.message, true); }
}
async function createTransfer() {
  const from   = Number(document.getElementById('tr-from').value);
  const to     = Number(document.getElementById('tr-to').value);
  const amount = document.getElementById('tr-amount').value;
  const mode   = document.getElementById('tr-mode').value;
  const desc   = document.getElementById('tr-desc').value.trim();
  if (!from || !to || !amount) { showMsg('From, to and amount are required', true); return; }
  try {
    const r = await api(txApi + '/transfer', {method:'POST', body:JSON.stringify({
      'from-account-id':from, 'to-account-id':to, amount, mode,
      'actor-user-id':currentUserId, description:desc
    })});
    await loadAccounts(); await loadPending();
    showMsg(r.message || 'Transfer submitted', false);
  } catch(e) { showMsg(e.message, true); }
}
async function loadPending() {
  try {
    const list  = await api(txApi + '/pending');
    const tbody = document.getElementById('pending-tbody');
    if (!list.length) {
      tbody.innerHTML = `<tr><td colspan='${canWrite?6:5}' style='color:#94a3b8'>No pending requests.</td></tr>`;
      return;
    }
    tbody.innerHTML = list.map(t => `<tr>
      <td style='color:#64748b;font-size:.8rem'>#${t.id}</td>
      <td>#${t['from-account-id']}</td>
      <td>#${t['to-account-id']}</td>
      <td style='font-variant-numeric:tabular-nums'>$${money(t.amount)}</td>
      <td><span class='pill pill-pending'>${esc(t.status)}</span></td>
      ${canWrite ? `<td class='actions'>
        <button class='btn-success' style='font-size:.76rem;padding:.25rem .55rem' onclick='handlePending(${t.id},\"approve\")'>Approve</button>
        <button class='btn-danger'  style='font-size:.76rem;padding:.25rem .55rem' onclick='handlePending(${t.id},\"reject\")'>Reject</button>
      </td>` : ''}
    </tr>`).join('');
  } catch(e) { showMsg(e.message, true); }
}
async function handlePending(id, decision) {
  try {
    const r = await api(txApi + '/' + id + '/' + decision, {
      method:'POST', body:JSON.stringify({'actor-user-id':currentUserId})
    });
    await loadAccounts(); await loadPending();
    showMsg(r.message || 'Transfer updated', false);
  } catch(e) { showMsg(e.message, true); }
}
async function loadHistory() {
  const accountId = Number(document.getElementById('history-account-id').value);
  if (!accountId) { showMsg('Enter an account ID', true); return; }
  try {
    const hist  = await api('/api/accounts/' + accountId + '/history');
    const tbody = document.getElementById('history-tbody');
    if (!hist.length) {
      tbody.innerHTML = `<tr><td colspan='5' style='color:#94a3b8'>No transactions found.</td></tr>`;
      return;
    }
    tbody.innerHTML = hist.map(h => `<tr>
      <td style='font-size:.78rem;color:#94a3b8'>${esc(h['created-at'])}</td>
      <td>${esc(h['entry-type'])}</td>
      <td style='font-variant-numeric:tabular-nums'>$${money(h.amount)}</td>
      <td style='font-variant-numeric:tabular-nums'>$${money(h['balance-after'])}</td>
      <td style='color:#64748b'>${esc(h.description || '')}</td>
    </tr>`).join('');
  } catch(e) { showMsg(e.message, true); }
}
async function boot() {
  try { await loadUsers(); await loadAccounts(); await loadPending(); }
  catch(e) { showMsg(e.message, true); }
}
boot();
")]])))
