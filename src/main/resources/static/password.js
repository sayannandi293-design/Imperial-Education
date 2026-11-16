(async()=>{
 const form=document.querySelector('#forgot-form')||document.querySelector('#reset-form');
 const status=document.querySelector('#password-message');
 try{
  const r=await fetch('/api/account/csrf');
  if(!r.ok)throw Error('Unable to load security token');
  const csrf=await r.json();
  form.addEventListener('submit',async e=>{
   e.preventDefault();status.textContent='Submitting…';
   const forgot=!!document.querySelector('#forgot-form');
   const token=new URLSearchParams(location.search).get('token');
   if(!forgot&&(!token||!/^[a-f0-9]{64}$/i.test(token))){status.textContent='Reset link is invalid. Request a new one.';return}
   const data=forgot?{email:document.querySelector('#forgot-email').value.trim()}:{token,password:document.querySelector('#reset-password').value};
   try{
    const response=await fetch('/api/account/password/'+(forgot?'forgot':'reset'),{method:'POST',credentials:'same-origin',headers:{'Content-Type':'application/json',[csrf.headerName]:csrf.token},body:JSON.stringify(data)});
    if(!response.ok)throw Error('Request failed. Check your details or contact support.');
    const result=await response.json();status.textContent=result.message;
    if(!forgot)form.reset();
   }catch(error){status.textContent=error.message}
  });
 }catch(e){status.textContent=e.message}
})();