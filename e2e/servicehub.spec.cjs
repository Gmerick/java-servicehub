const { test, expect } = require("@playwright/test");
async function navigate(page, name) {
  await page
    .getByRole("navigation")
    .getByRole("button", { name, exact: true })
    .click();
}
test("percurso completo: cliente, equipamento, orçamento, aprovação e entrega", async ({
  page,
}) => {
  const errors = [];
  page.on("pageerror", (e) => errors.push(e.message));
  await page.goto("/");
  await navigate(page, "Clientes");
  await page
    .getByRole("button", { name: "Novo cliente", exact: false })
    .click();
  await page.getByLabel("Nome", { exact: true }).fill("Cliente Navegador");
  await page
    .getByLabel("E-mail", { exact: true })
    .fill("navegador@example.com");
  await page.getByLabel("Telefone", { exact: true }).fill("11912345678");
  await page.getByRole("button", { name: "Salvar", exact: true }).click();
  await expect(page.getByRole("dialog")).not.toBeVisible();
  await navigate(page, "Equipamentos");
  await page
    .getByRole("button", { name: "Novo equipamento", exact: false })
    .click();
  await page
    .getByLabel("Cliente", { exact: true })
    .selectOption({ label: "Cliente Navegador" });
  await page.getByLabel("Equipamento", { exact: true }).fill("Notebook teste");
  await page.getByLabel("Número de série", { exact: true }).fill("UI-NB-01");
  await page.getByRole("button", { name: "Salvar", exact: true }).click();
  await expect(page.getByRole("dialog")).not.toBeVisible();
  await navigate(page, "Ordens de serviço");
  await page.getByRole("button", { name: "Nova ordem", exact: false }).click();
  await page
    .getByLabel("Equipamento / cliente")
    .selectOption({ label: "Notebook teste · Cliente Navegador" });
  await page.getByLabel("Título do atendimento").fill("Revisão via navegador");
  await page.getByLabel("Prazo de entrega").fill("2099-12-31");
  await page
    .getByLabel("Descrição do problema")
    .fill("Revisar notebook e instalar SSD.");
  await page.getByRole("button", { name: "Salvar", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: /Revisão via navegador/ }),
  ).toBeVisible();
  await page.getByLabel("Descrição do serviço").fill("Mão de obra");
  await page.getByLabel("Valor unitário (R$)").fill("150.00");
  await page.getByRole("button", { name: "Adicionar ao orçamento" }).click();
  await expect(page.locator(".total")).toContainText("150,00");
  await page
    .getByLabel("Tipo de item")
    .selectOption({ label: "SSD 480 GB · R$ 249,90 · 8 un." });
  await page.getByRole("button", { name: "Adicionar ao orçamento" }).click();
  await expect(page.locator(".total")).toContainText("399,90");
  for (const [value, note] of [
    ["APPROVED", "Cliente aprovou orçamento"],
    ["IN_PROGRESS", "Instalação iniciada"],
    ["COMPLETED", "SSD instalado e teste concluído"],
  ]) {
    await page.getByLabel("Novo status").selectOption(value);
    await page.getByLabel("Observação / resolução").fill(note);
    await page.getByRole("button", { name: "Confirmar etapa" }).click();
    await expect(page.locator(".detail-meta")).toContainText(
      {
        APPROVED: "Aprovada",
        IN_PROGRESS: "Em execução",
        COMPLETED: "Concluída",
      }[value],
    );
  }
  await expect(
    page.getByRole("heading", { name: "Encerramento" }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Adicionar ao orçamento" }),
  ).toHaveCount(0);
  await page.getByRole("button", { name: "Fechar", exact: true }).click();
  await navigate(page, "Peças e estoque");
  await expect(
    page.getByRole("row").filter({ hasText: "SSD 480 GB" }),
  ).toContainText("7 un.");
  expect(errors).toEqual([]);
});
test("validação, erro de duplicidade e recuperação", async ({ page }) => {
  await page.goto("/");
  await navigate(page, "Clientes");
  await page
    .getByRole("button", { name: "Novo cliente", exact: false })
    .click();
  await page.getByRole("button", { name: "Salvar", exact: true }).click();
  await expect(page.getByRole("dialog")).toBeVisible();
  await page.getByLabel("Nome", { exact: true }).fill("Outro");
  await page.getByLabel("E-mail", { exact: true }).fill("aurora@example.com");
  await page.getByLabel("Telefone", { exact: true }).fill("11");
  await page.getByRole("button", { name: "Salvar", exact: true }).click();
  await expect(page.locator(".form-error")).toContainText("duplicado");
  await page.getByLabel("E-mail", { exact: true }).fill("outro@example.com");
  await page.getByRole("button", { name: "Salvar", exact: true }).click();
  await expect(page.getByRole("dialog")).not.toBeVisible();
});
test("cancelar retorna estoque uma vez e filtro mostra resultado", async ({
  page,
}) => {
  await page.goto("/");
  await navigate(page, "Ordens de serviço");
  await page
    .getByRole("button", { name: "Abrir ordem 1", exact: true })
    .click();
  await page.getByLabel("Novo status").selectOption("APPROVED");
  await page.getByLabel("Observação / resolução").fill("Orçamento aprovado");
  await page.getByRole("button", { name: "Confirmar etapa" }).click();
  await expect(page.locator(".detail-meta")).toContainText("Aprovada");
  await page.getByLabel("Novo status").selectOption("CANCELED");
  await page.getByLabel("Observação / resolução").fill("Cliente desistiu");
  await page.getByRole("button", { name: "Confirmar etapa" }).click();
  await expect(page.locator(".detail-meta")).toContainText("Cancelada");
  await expect(
    page.getByRole("button", { name: "Confirmar etapa" }),
  ).toHaveCount(0);
  await page.getByRole("button", { name: "Fechar", exact: true }).click();
  await page.getByLabel("Filtrar status").selectOption("CANCELED");
  await page.getByRole("button", { name: "Filtrar", exact: true }).click();
  await expect(
    page.getByRole("row").filter({ hasText: "Upgrade de armazenamento" }),
  ).toContainText("Cancelada");
});
test("erro de rede oferece recuperação", async ({ page }) => {
  await page.route("**/api/dashboard", (r) => r.abort());
  await page.goto("/");
  await expect(page.getByRole("alert")).toContainText("Servidor indisponível");
  await page.unroute("**/api/dashboard");
  await page.getByRole("button", { name: "Tentar novamente" }).click();
  await expect(
    page.getByRole("heading", { name: "Cada serviço, sob controle." }),
  ).toBeVisible();
});
test("CSV faz download e estoque permite reposição", async ({ page }) => {
  await page.goto("/");
  await navigate(page, "Peças e estoque");
  await page
    .getByRole("row")
    .filter({ hasText: "Memória DDR4" })
    .getByRole("button", { name: "Repor", exact: true })
    .click();
  await page.getByLabel("Quantidade de entrada").fill("3");
  await page.getByLabel("Motivo da entrada").fill("Compra teste");
  await page.getByRole("button", { name: "Salvar", exact: true }).click();
  await expect(page.getByRole("dialog")).not.toBeVisible();
  await expect(
    page.getByRole("row").filter({ hasText: "Memória DDR4" }),
  ).toContainText("5 un.");
  await navigate(page, "Ordens de serviço");
  const download = page.waitForEvent("download");
  await page.getByRole("link", { name: "Exportar CSV" }).click();
  expect((await download).suggestedFilename()).toBe("servicehub-ordens.csv");
});
test("desktop e celular: navegação, modal e evidência visual", async ({
  page,
}) => {
  await page.goto("/");
  await expect(
    page.getByRole("heading", { name: "Cada serviço, sob controle." }),
  ).toBeVisible();
  await page.screenshot({
    path: "docs/screenshots/desktop.png",
    fullPage: true,
  });
  await page.setViewportSize({ width: 390, height: 844 });
  await page.screenshot({
    path: "docs/screenshots/mobile.png",
    fullPage: true,
  });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  await navigate(page, "Ordens de serviço");
  await page.getByRole("button", { name: "Nova ordem", exact: false }).click();
  await expect(page.getByLabel("Descrição do problema")).toBeVisible();
  await page.keyboard.press("Escape");
  await expect(page.getByRole("dialog")).not.toBeVisible();
});
test("conteúdo digitado permanece texto, sem executar HTML", async ({
  page,
  request,
}) => {
  await request.post("/api/customers", {
    data: {
      name: "<img src=x onerror=alert(1)>",
      email: "xss@example.com",
      phone: "11",
    },
  });
  await page.goto("/");
  await navigate(page, "Clientes");
  await expect(
    page.getByText("<img src=x onerror=alert(1)>", { exact: true }),
  ).toBeVisible();
  expect(await page.locator("td img").count()).toBe(0);
});
