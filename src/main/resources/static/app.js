"use strict";
const $ = (s) => document.querySelector(s),
  esc = (v) =>
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
const money = (v) =>
  Number(v).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
const date = (v) =>
  v
    ? new Date(v.length === 10 ? v + "T12:00:00" : v).toLocaleDateString(
        "pt-BR",
      )
    : "—";
const labels = {
  DRAFT: "Rascunho",
  APPROVED: "Aprovada",
  IN_PROGRESS: "Em execução",
  COMPLETED: "Concluída",
  CANCELED: "Cancelada",
  LOW: "Baixa",
  NORMAL: "Normal",
  HIGH: "Alta",
};
const titles = {
  dashboard: "Visão geral",
  orders: "Ordens de serviço",
  customers: "Clientes",
  assets: "Equipamentos",
  parts: "Peças e estoque",
  guide: "Guia de uso",
};
let admin = false, csrfToken = null;
let view = "dashboard",
  currentPage = 0,
  search = "",
  status = "",
  customers = [],
  assets = [],
  parts = [],
  selected = null,
  toastTimer,
  renderVersion = 0;
const badge = (v) =>
  `<span class="badge ${esc(v)}">${esc(labels[v] || v)}</span>`;
async function api(path, method = "GET", body) {
  const res = await fetch("/api" + path, {
    method,
    headers: { ...(body ? { "Content-Type": "application/json" } : {}), ...(method !== "GET" ? { [csrfToken.headerName]: csrfToken.token } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (res.status === 401) { location.replace("/login.html?expired=1"); throw Error("Sessão encerrada."); }
  let data;
  try {
    data = await res.json();
  } catch {
    throw Error("Não foi possível ler a resposta do servidor.");
  }
  if (!res.ok)
    throw Error(data.message || "Não foi possível concluir a operação.");
  return data;
}
function toast(text) {
  clearTimeout(toastTimer);
  $("#toast").textContent = text;
  toastTimer = setTimeout(() => ($("#toast").textContent = ""), 4500);
}
function error(text) {
  const node = $("#error");
  node.hidden = !text;
  node.textContent = text;
  if (text) {
    const b = document.createElement("button");
    b.className = "secondary";
    b.textContent = "Tentar novamente";
    b.onclick = () => render();
    node.append(b);
  }
}
const heading = (title, sub, action = "") =>
  `<div class="page-heading"><div><span class="eyebrow">SERVICEHUB / BANCADA</span><h1>${title}</h1><p>${sub}</p></div>${action}</div>`;
const createButton = (type, title) =>
  admin ? `<button class="primary" data-new="${type}">＋ ${title}</button>` : "";
const table = (headers, rows) =>
  `<div class="table-wrap"><table><thead><tr>${headers.map((x) => `<th>${x}</th>`).join("")}</tr></thead><tbody>${rows || `<tr><td colspan="${headers.length}" class="empty">Nenhum registro encontrado. Cadastre o primeiro ou ajuste os filtros.</td></tr>`}</tbody></table></div>`;
function orderRows(rows) {
  return rows
    .map(
      (o) =>
        `<tr><td><strong>#${String(o.id).padStart(4, "0")}</strong><small>${date(o.createdAt)}</small></td><td><strong>${esc(o.title)}</strong><small>${esc(o.customerName)}</small></td><td>${badge(o.status)}</td><td class="money nowrap">${money(o.total)}</td><td><button class="text-button" data-order="${o.id}" aria-label="Abrir ordem ${o.id}">Ver ordem →</button></td></tr>`,
    )
    .join("");
}
async function referenceData() {
  [customers, assets, parts] = await Promise.all([
    api("/customers"),
    api("/assets"),
    api("/parts"),
  ]);
}
async function render() {
  const version = ++renderVersion;
  error("");
  $("#crumb").textContent = titles[view];
  document
    .querySelectorAll("[data-view]")
    .forEach((b) => b.classList.toggle("active", b.dataset.view === view));
  $("#page").innerHTML =
    '<div class="loading" role="status">Organizando a bancada…</div>';
  try {
    let html = "";
    if (view === "dashboard") {
      const d = await api("/dashboard");
      html = `<div class="desk-heading"><div><span class="eyebrow">SUA BANCADA / VISÃO GERAL</span><h1>Vamos ao próximo serviço.</h1><p>Retome um atendimento ou abra espaço para o próximo.</p></div>${createButton("order", "Nova ordem")}</div>
        <div class="desk-layout">
          <section class="work-surface">
            <div class="live-overview"><div><span class="eyebrow">EM ABERTO</span><div class="open-count">${d.open}<span>ordem${d.open === 1 ? "" : "s"}<br>na bancada</span></div></div><div class="signal-art" aria-hidden="true"><span class="signal-ring ring-one"></span><span class="signal-ring ring-two"></span><span class="signal-core">sh<span> / </span></span><i class="signal-dot"></i></div><div class="deadline-note"><span class="status-dot"></span><b>${d.overdue ? `${d.overdue} prazo(s) vencido(s)` : "Prazos em dia"}</b><small>${d.overdue ? "Confira as ordens abertas antes de continuar." : "Nenhuma ordem aberta está atrasada."}</small></div></div>
            <div class="section-heading"><div><span class="eyebrow">01 / ATENDIMENTOS</span><h2>O trabalho continua aqui.</h2></div><button class="text-button" data-view="orders">Todas as ordens <span aria-hidden="true">↗</span></button></div>
            <div class="recent-list">${d.recent.map((o) => `<button class="service-row" data-order="${o.id}" aria-label="Abrir ordem ${o.id}"><span class="order-index">${String(o.id).padStart(3, "0")}</span><span class="service-info"><strong>${esc(o.title)}</strong><small>${esc(o.customerName)} <span>· ${esc(o.assetName)}</span></small></span>${badge(o.status)}<span class="row-value">${money(o.total)}</span><span class="row-arrow" aria-hidden="true">↗</span></button>`).join("") || '<div class="empty">Sua bancada está livre. Abra a primeira ordem para começar.</div>'}</div>
            <div class="desk-summary"><div><span class="summary-number">${d.completed}</span><span>serviços<br>concluídos</span></div><div><span class="summary-money">${money(d.completedValue)}</span><span>em orçamentos concluídos<small>Não representa recebimentos.</small></span></div></div>
          </section>
          <aside class="desk-rail"><section class="stock-panel"><div class="rail-caption"><span class="eyebrow">02 / ESTOQUE</span><span aria-hidden="true">↗</span></div><div class="stock-number">${String(d.lowStock).padStart(2, "0")}<span>peça(s) para<br>acompanhar</span></div><p>${d.lowStock ? "Itens no estoque mínimo ou abaixo dele. Confira antes de aprovar um orçamento." : "As peças estão acima do mínimo. Você pode consultar o saldo no catálogo."}</p><button class="secondary" data-view="parts">Conferir estoque <span aria-hidden="true">→</span></button><div class="stock-track" aria-hidden="true"><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i></div></section>
          <section class="shortcuts"><span class="eyebrow">ATALHOS DA BANCADA</span><button data-new="customer"><span>Novo cliente<small>O primeiro contato</small></span><span aria-hidden="true">＋</span></button><button data-new="asset"><span>Novo equipamento<small>Vincular ao cliente</small></span><span aria-hidden="true">＋</span></button><button data-view="guide"><span>Como funciona<small>Da entrada à entrega</small></span><span aria-hidden="true">↗</span></button></section><p class="rail-footnote"><span class="status-dot"></span>Dados protegidos no servidor.<br><span>Seu ritmo. Seu espaço de trabalho.</span></p></aside>
        </div>`;
    } else if (view === "orders") {
      const d = await api(
        `/orders?page=${currentPage}&size=10&search=${encodeURIComponent(search)}${status ? "&status=" + status : ""}`,
      );
      html =
        heading(
          titles[view],
          "Orçamentos, execução e histórico em um só lugar.",
          createButton("order", "Nova ordem"),
        ) +
        `<section class="card"><form id="filters" class="toolbar"><input name="search" aria-label="Buscar ordens" placeholder="Buscar por título, cliente ou número" value="${esc(search)}"><select name="status" aria-label="Filtrar status"><option value="">Todos os status</option>${Object.keys(
          labels,
        )
          .slice(0, 5)
          .map(
            (k) =>
              `<option value="${k}" ${status === k ? "selected" : ""}>${labels[k]}</option>`,
          )
          .join(
            "",
          )}</select><button class="secondary">Filtrar</button><a class="secondary" href="/api/orders.csv" download>Exportar CSV</a></form>${table(["Ordem", "Atendimento", "Status", "Orçamento", ""], orderRows(d.items))}<div class="pagination"><span>${d.total} ordem(ns) · Página ${currentPage + 1}</span><div><button class="secondary" data-page="${currentPage - 1}" ${currentPage === 0 ? "disabled" : ""}>Anterior</button><button class="secondary" data-page="${currentPage + 1}" ${(currentPage + 1) * 10 >= d.total ? "disabled" : ""}>Próxima</button></div></div></section>`;
    } else if (view === "customers") {
      customers = await api("/customers");
      html =
        heading(
          "Clientes",
          "O ponto de partida de cada atendimento.",
          createButton("customer", "Novo cliente"),
        ) +
        `<section class="card">${table(["Cliente", "E-mail", "Telefone"], customers.map((c) => `<tr><td><strong>${esc(c.name)}</strong><small>#${c.id}</small></td><td>${esc(c.email)}</td><td>${esc(c.phone)}</td></tr>`).join(""))}</section>`;
    } else if (view === "assets") {
      assets = await api("/assets");
      html =
        heading(
          "Equipamentos",
          "Cada equipamento vinculado ao cliente certo.",
          createButton("asset", "Novo equipamento"),
        ) +
        `<section class="card">${table(["Equipamento", "Número de série", "Cliente"], assets.map((a) => `<tr><td><strong>${esc(a.name)}</strong></td><td>${esc(a.serial)}</td><td>${esc(a.customerName)}</td></tr>`).join(""))}</section>`;
    } else if (view === "parts") {
      parts = await api("/parts");
      html =
        heading(
          "Peças e estoque",
          "Disponibilidade atualizada a cada aprovação e cancelamento.",
          createButton("part", "Nova peça"),
        ) +
        `<section class="card">${table(["Peça / SKU", "Preço unitário", "Disponível", "Estoque mínimo", ""], parts.map((p) => `<tr><td><strong>${esc(p.name)}</strong><small>${esc(p.sku)}</small></td><td class="money">${money(p.price)}</td><td><strong>${p.stock} un.</strong>${p.stock <= p.minimum ? '<span class="badge HIGH">Reposição sugerida</span>' : ""}</td><td>${p.minimum} un.</td><td><button class="text-button" data-restock="${p.id}">Repor</button><button class="text-button" data-movements="${p.id}">Histórico</button></td></tr>`).join(""))}<div class="list-note">Peças são retiradas ao aprovar a ordem. Cancelar uma ordem aprovada ou em execução devolve o estoque.</div></section>`;
    } else
      html =
        heading(
          "Seu guia de uso",
          "Um fluxo simples para demonstrar o projeto com confiança.",
        ) +
        `<div class="help-content"><section class="card"><h2>Comece em cinco passos</h2><ol><li>Em <b>Clientes</b>, cadastre nome, e-mail e telefone.</li><li>Em <b>Equipamentos</b>, vincule um equipamento ao cliente.</li><li>Em <b>Ordens de serviço</b>, abra uma ordem com prazo e prioridade.</li><li>Abra a ordem e inclua <b>serviços e peças</b> no orçamento. As peças usam o preço do catálogo.</li><li><b>Aprove → Inicie → Conclua</b>, registrando uma observação em cada etapa.</li></ol></section><section class="card"><h2>Regras que protegem a operação</h2><p>Somente rascunhos permitem alterar itens. Aprovar exige ao menos um item e estoque suficiente para todas as peças. Se faltar uma peça, nada é retirado. Ordens concluídas e canceladas ficam somente para consulta.</p><p>O painel mostra o valor dos orçamentos concluídos, sem representar recebimentos. O CSV exporta todas as ordens.</p></section><section class="card"><h2>Acesso e demonstração</h2><p>A demonstração usa somente dados fictícios. Visitantes podem consultar painéis, cadastros, estoque e históricos. Cadastros e alterações exigem acesso administrativo.</p><p>Use Sair para encerrar sua sessão. Se o acesso expirar, entre novamente. Não informe dados pessoais reais na demonstração. O roteiro de apresentação e as instruções de operação estão na documentação do projeto.</p></section></div>`;
    if (version === renderVersion) { $("#page").innerHTML = html; applyRole(); }
  } catch (e) {
    if (version === renderVersion) {
      $("#page").innerHTML =
        '<div class="empty">Não foi possível carregar os dados.</div>';
      error(
        e.message === "Failed to fetch"
          ? "Servidor indisponível. Confira se o ServiceHub está em execução."
          : e.message,
      );
    }
  }
}
function openDialog(title, sub, body) {
  $("#dialog-content").innerHTML =
    `<div class="dialog-header"><div><h2 id="dialog-title">${esc(title)}</h2><p>${esc(sub)}</p></div><button class="close" data-close aria-label="Fechar">×</button></div><div class="dialog-body"><div class="form-error" role="alert"></div>${body}</div>`;
  if (!$("#modal").open) $("#modal").showModal();
}
const input = (label, name, type = "text", extra = "") =>
  `<label>${label}<input name="${name}" type="${type}" required ${extra}></label>`;
const options = (items, label) =>
  items
    .map((i) => `<option value="${i.id}">${esc(label(i))}</option>`)
    .join("");
const select = (label, name, opts) =>
  `<label>${label}<select name="${name}" required><option value="">Selecione</option>${opts}</select></label>`;
async function newForm(type, id) {
  if (!admin) return;
  await referenceData();
  let fields = "",
    title = "";
  if (type === "customer") {
    title = "Novo cliente";
    fields =
      input("Nome", "name", "text", 'maxlength="100"') +
      input("E-mail", "email", "email", 'maxlength="160"') +
      input("Telefone", "phone", "tel", 'maxlength="30"');
  }
  if (type === "asset") {
    if (!customers.length) {
      toast("Cadastre primeiro um cliente.");
      return;
    }
    title = "Novo equipamento";
    fields =
      select(
        "Cliente",
        "customerId",
        options(customers, (c) => c.name),
      ) +
      input("Equipamento", "name", "text", 'maxlength="100"') +
      input("Número de série", "serial", "text", 'maxlength="80"');
  }
  if (type === "part") {
    title = "Nova peça";
    fields =
      input("Nome da peça", "name", "text", 'maxlength="100"') +
      input("SKU", "sku", "text", 'maxlength="40"') +
      input(
        "Preço unitário (R$)",
        "price",
        "number",
        'min="0" max="9999999.99" step="0.01"',
      ) +
      input(
        "Estoque inicial",
        "stock",
        "number",
        'min="0" max="1000000" step="1"',
      ) +
      input(
        "Estoque mínimo",
        "minimum",
        "number",
        'min="0" max="1000000" step="1"',
      );
  }
  if (type === "order") {
    if (!assets.length) {
      toast("Cadastre primeiro um cliente e um equipamento.");
      return;
    }
    title = "Nova ordem de serviço";
    const today = new Date();
    today.setMinutes(today.getMinutes() - today.getTimezoneOffset());
    fields =
      select(
        "Equipamento / cliente",
        "assetId",
        options(assets, (a) => a.name + " · " + a.customerName),
      ) +
      input("Título do atendimento", "title", "text", 'maxlength="140"') +
      select(
        "Prioridade",
        "priority",
        '<option value="LOW">Baixa</option><option value="NORMAL" selected>Normal</option><option value="HIGH">Alta</option>',
      ) +
      input(
        "Prazo de entrega",
        "dueDate",
        "date",
        `min="${today.toISOString().slice(0, 10)}"`,
      ) +
      `<label class="full">Descrição do problema<textarea name="description" required maxlength="2000"></textarea></label>`;
  }
  if (type === "restock") {
    title = "Repor estoque";
    fields =
      input(
        "Quantidade de entrada",
        "quantity",
        "number",
        'min="1" max="100000" step="1"',
      ) + input("Motivo da entrada", "reason", "text", 'maxlength="160"');
  }
  openDialog(
    title,
    "Preencha os campos para continuar.",
    `<form data-form="${type}" data-id="${id || ""}"><div class="form-grid">${fields}</div><div class="form-actions"><button type="button" class="secondary" data-close>Cancelar</button><button class="primary" type="submit">Salvar</button></div></form>`,
  );
}
async function showOrder(id) {
  await referenceData();
  selected = await api("/orders/" + id);
  const o = selected.order;
  const draft = admin && o.status === "DRAFT";
  const actions = !admin ? [] :
    {
      DRAFT: ["APPROVED", "CANCELED"],
      APPROVED: ["IN_PROGRESS", "CANCELED"],
      IN_PROGRESS: ["COMPLETED", "CANCELED"],
    }[o.status] || [];
  openDialog(
    `#${String(id).padStart(4, "0")} · ${o.title}`,
    o.customerName + " / " + o.assetName,
    `<div class="detail-meta">${badge(o.status)}${badge(o.priority)}<span>Prazo: ${date(o.dueDate)}</span></div><p class="wrap">${esc(o.description)}</p><section class="detail-section"><h3>Orçamento</h3>${table(["Item", "Qtd.", "Unitário", "Total", ""], selected.items.map((i) => `<tr><td class="wrap">${esc(i.description)}<small>${i.partId ? "Peça do catálogo" : "Serviço"}</small></td><td>${i.quantity}</td><td class="nowrap">${money(i.unitPrice)}</td><td class="nowrap">${money(i.total)}</td><td>${draft ? `<button class="text-button" data-remove-item="${i.id}" aria-label="Remover item ${i.id}">Remover</button>` : ""}</td></tr>`).join(""))}<div class="total"><span>Total do orçamento</span><b>${money(o.total)}</b></div></section>${draft ? `<section class="detail-section"><h3>Adicionar item</h3><form data-form="item" data-id="${id}"><div class="form-grid"><label>Tipo de item<select name="partId" id="item-part"><option value="">Serviço / mão de obra</option>${options(parts, (p) => p.name + " · " + money(p.price) + " · " + p.stock + " un.")}</select></label>${input("Descrição do serviço", "description", "text", 'maxlength="160"')}${input("Quantidade", "quantity", "number", 'value="1" min="1" max="1000" step="1"')}${input("Valor unitário (R$)", "unitPrice", "number", 'min="0" max="9999999.99" step="0.01"')}</div><div class="form-actions"><button class="secondary" type="submit">Adicionar ao orçamento</button></div></form></section>` : ""}${actions.length ? `<section class="detail-section"><h3>Próxima etapa</h3><form data-form="transition" data-id="${id}"><div class="form-grid">${select("Novo status", "status", actions.map((a) => `<option value="${a}">${labels[a]}</option>`).join(""))}<label>Observação / resolução<textarea name="note" required maxlength="2000" placeholder="Registre a aprovação, o início, a resolução ou o motivo do cancelamento"></textarea></label></div><p><small>Aprovar retira as peças do estoque. Cancelar devolve as peças de ordens já aprovadas.</small></p><div class="form-actions"><button class="primary" type="submit">Confirmar etapa</button></div></form></section>` : `<section class="detail-section"><h3>${admin ? "Encerramento" : "Modo de consulta"}</h3><p class="wrap">${esc(o.resolution || (admin ? "Ordem cancelada. Consulte o histórico para ver o motivo." : "Alterações de orçamento e etapas exigem administração."))}</p></section>`}<section class="detail-section"><h3>Histórico da ordem</h3><ul class="timeline">${selected.events.map((e) => `<li>${esc(e.message)}<small>${date(e.createdAt)} · ${esc(e.createdAt.slice(11, 16))}</small></li>`).join("")}</ul></section>`,
  );
}
async function submitForm(form) {
  if (!admin) return;
  const data = Object.fromEntries(new FormData(form));
  const type = form.dataset.form,
    id = form.dataset.id;
  for (const key of [
    "customerId",
    "assetId",
    "price",
    "stock",
    "minimum",
    "quantity",
    "unitPrice",
  ])
    if (key in data) data[key] = Number(data[key]);
  if (type === "item") data.partId = data.partId ? Number(data.partId) : null;
  const endpoint = {
    customer: "/customers",
    asset: "/assets",
    part: "/parts",
    order: "/orders",
    restock: `/parts/${id}/restock`,
    item: `/orders/${id}/items`,
    transition: `/orders/${id}/status`,
  }[type];
  form.querySelectorAll("button").forEach((b) => (b.disabled = true));
  $(".form-error").textContent = "";
  try {
    const result = await api(endpoint, "POST", data);
    toast("Alteração salva com sucesso.");
    if (type === "item" || type === "transition") {
      await showOrder(Number(id));
      await render();
    } else if (type === "order") {
      await render();
      await showOrder(result.order.id);
    } else {
      $("#modal").close();
      await render();
    }
  } catch (e) {
    $(".form-error").textContent = e.message;
    form.querySelectorAll("button").forEach((b) => (b.disabled = false));
  }
}
document.addEventListener("submit", (e) => {
  e.preventDefault();
  if (e.target.id === "filters") {
    const d = new FormData(e.target);
    search = d.get("search");
    status = d.get("status");
    currentPage = 0;
    render();
  } else if (e.target.dataset.form) submitForm(e.target);
});
document.addEventListener("change", (e) => {
  if (e.target.id === "item-part") {
    const form = e.target.form,
      p = parts.find((p) => p.id === Number(e.target.value));
    form.elements.description.value = p ? p.name : "";
    form.elements.unitPrice.value = p ? p.price : "";
    form.elements.description.readOnly = !!p;
    form.elements.unitPrice.readOnly = !!p;
  }
});
document.addEventListener("click", async (e) => {
  const b = e.target.closest("button");
  if (!b || b.disabled) return;
  try {
    if (b.hasAttribute("data-close")) {
      $("#modal").close();
      return;
    }
    if (b.dataset.view) {
      view = b.dataset.view;
      currentPage = 0;
      await render();
      return;
    }
    if (b.dataset.new) {
      b.disabled = true;
      try {
        await newForm(b.dataset.new);
      } finally {
        b.disabled = false;
      }
      return;
    }
    if (b.dataset.order) {
      await showOrder(Number(b.dataset.order));
      return;
    }
    if (b.dataset.page) {
      currentPage = Number(b.dataset.page);
      await render();
      return;
    }
    if (b.dataset.restock) {
      await newForm("restock", b.dataset.restock);
      return;
    }
    if (b.dataset.removeItem) {
      b.disabled = true;
      await api(
        `/orders/${selected.order.id}/items/${b.dataset.removeItem}`,
        "DELETE",
      );
      await showOrder(selected.order.id);
      await render();
      return;
    }
    if (b.dataset.movements) {
      const records = await api(`/parts/${b.dataset.movements}/movements`);
      openDialog(
        "Movimentações de estoque",
        "Entradas, aprovações e cancelamentos.",
        table(
          ["Data", "Quantidade", "Motivo", "Ordem"],
          records
            .map(
              (r) =>
                `<tr><td>${date(r.CREATED_AT)}</td><td>${r.DELTA > 0 ? "+" : ""}${r.DELTA}</td><td>${esc(r.REASON)}</td><td>${r.ORDER_ID ? "#" + r.ORDER_ID : "—"}</td></tr>`,
            )
            .join(""),
        ),
      );
    }
  } catch (err) {
    if ($("#modal").open && $(".form-error"))
      $(".form-error").textContent = err.message;
    else error(err.message);
    b.disabled = false;
  }
});
function applyRole() {
  if(!admin) document.querySelectorAll('[data-new],[data-restock],[data-remove-item],form[data-form]').forEach(el=>el.remove());
}
async function initialize() {
  try {
    const session=await api('/session');
    admin=session.admin;
    csrfToken=await api('/csrf');
    $('#session-label').textContent=admin?'Administração':'Visitante · somente consulta';
    $('#demo-banner').hidden=!session.demo;
    $('#demo-role').textContent=admin?'Acesso administrativo. Use somente informações fictícias.':'Você pode consultar todos os recursos. Cadastros e alterações exigem administração.';
    await render();
  } catch(e) {error(e.message);}
}
$('#logout').onclick=async()=>{
  $('#logout').disabled=true;
  try {
    csrfToken=await api('/csrf');
    const response=await fetch('/logout',{method:'POST',headers:{[csrfToken.headerName]:csrfToken.token}});
    if(!response.ok)throw Error('Não foi possível encerrar a sessão. Atualize e tente novamente.');
    $('#page').replaceChildren();location.replace('/login.html');
  }catch(e){error(e.message);$('#logout').disabled=false;}
};
initialize();
