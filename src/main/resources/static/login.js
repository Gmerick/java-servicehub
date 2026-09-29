"use strict";
let csrf;
const form=document.querySelector('#login-form'), error=document.querySelector('#login-error'), status=document.querySelector('#login-status');
function busy(value,text=''){document.querySelectorAll('button').forEach(b=>b.disabled=value);status.textContent=text;form.setAttribute('aria-busy',String(value));}
async function refresh(){const r=await fetch('/api/csrf',{cache:'no-store'});if(!r.ok)throw Error('Não foi possível iniciar uma sessão segura.');csrf=await r.json();document.querySelector('#visitor-access').hidden=!csrf.demo;}
async function enter(visitor){
  error.hidden=true;busy(true,visitor?'Abrindo demonstração…':'Verificando acesso…');
  try{
    await refresh();
    const r=await fetch(visitor?'/demo/visit':'/login',{method:'POST',headers:{[csrf.headerName]:csrf.token,...(!visitor?{'Content-Type':'application/x-www-form-urlencoded'}:{})},body:visitor?undefined:new URLSearchParams(new FormData(form))});
    if(!r.ok){const data=await r.json();throw Error(data.message || 'Não foi possível entrar. Tente novamente.');}
    location.replace('/');
  }catch(e){error.textContent=e.message==='Failed to fetch'?'Servidor indisponível. Confira sua conexão e tente novamente.':e.message;error.hidden=false;busy(false);}
}
form.addEventListener('submit',e=>{e.preventDefault();enter(false);});document.querySelector('#visit').onclick=()=>enter(true);
refresh().then(()=>busy(false)).catch(e=>{error.textContent=e.message+' Recarregue a página para tentar novamente.';error.hidden=false;});
if(new URLSearchParams(location.search).has('expired')){error.textContent='Sua sessão expirou. Entre novamente para continuar.';error.hidden=false;}
