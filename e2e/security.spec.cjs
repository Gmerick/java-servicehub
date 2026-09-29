const {test,expect}=require('@playwright/test');
const auth=require('../target/test-auth.json');
test('login inválido, estado de carregamento e logout',async({page})=>{
 await page.goto('/login.html');
 await page.getByLabel('Usuário',{exact:true}).fill(auth.username);await page.getByLabel('Senha',{exact:true}).fill('incorreta');
 await page.getByRole('button',{name:'Entrar',exact:true}).click();await expect(page.getByRole('alert')).toContainText('Usuário ou senha inválidos');
 await page.getByLabel('Senha',{exact:true}).fill(auth.password);await page.getByRole('button',{name:'Entrar',exact:true}).click();
 await expect(page.getByText('Administração',{exact:true})).toBeVisible();await page.getByRole('button',{name:'Sair',exact:true}).click();
 await expect(page).toHaveURL(/login.html/);expect((await page.request.get('/api/customers')).status()).toBe(401);
});
for(const [name,viewport] of [['desktop',{width:1440,height:1000}],['mobile',{width:390,height:844}]]){
 test(`visitante e login ${name}`,async({page})=>{
  await page.setViewportSize(viewport);await page.goto('/login.html');await expect(page.getByRole('button',{name:'Explorar como visitante'})).toBeEnabled();
  await page.screenshot({path:`docs/screenshots/demo-login-${name}.png`,fullPage:true,animations:'disabled'});
  await page.getByRole('button',{name:'Explorar como visitante'}).click();await expect(page.getByRole('heading',{name:'Vamos ao próximo serviço.'})).toBeVisible();
  await expect(page.getByText('Ambiente de demonstração — dados fictícios',{exact:true})).toBeVisible();
  await expect(page.locator('[data-new]')).toHaveCount(0);
  const csrf=await (await page.request.get('/api/csrf')).json();
  expect((await page.request.post('/api/customers',{headers:{[csrf.headerName]:csrf.token},data:{name:'Proibido'}})).status()).toBe(403);
  await page.screenshot({path:`docs/screenshots/demo-visitor-${name}.png`,fullPage:true,animations:'disabled'});
  for(const name of ['Clientes','Equipamentos','Peças e estoque','Ordens de serviço']) {await page.getByRole('navigation').getByRole('button',{name,exact:true}).click();await expect(page.getByRole('heading',{name,exact:true})).toBeVisible();}
  await page.locator('[data-order]').first().click();await expect(page.getByRole('heading',{name:'Histórico da ordem'})).toBeVisible();await expect(page.locator('form[data-form]')).toHaveCount(0);
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
 });
}
