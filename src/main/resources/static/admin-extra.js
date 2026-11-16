(async()=>{
 // Obtain a current CSRF token for each write. Do not bypass Spring Security.
 async function getCsrf(){
  const r=await fetch('/api/admin/csrf',{credentials:'same-origin',cache:'no-store',redirect:'manual'});
  if(!r.ok||r.type==='opaqueredirect')throw Error('Admin session expired. Please sign in again.');
  return r.json();
 }
 try{await getCsrf()}catch(e){return}
 async function api(path,method='GET',body){
  const headers={'Accept':'application/json'};
  if(method!=='GET'){
   const csrf=await getCsrf();
   headers[csrf.headerName]=csrf.token;
  }
  if(body)headers['Content-Type']='application/json';
  const r=await fetch(path,{method,credentials:'same-origin',cache:'no-store',redirect:'manual',headers,body:body?JSON.stringify(body):undefined});
  if(r.type==='opaqueredirect'||r.status===401)throw Error('Admin session expired. Please log in again.');
  if(r.status===403)throw Error('Access denied (403). Refresh and sign in as admin again, then retry.');
  if(!r.ok)throw Error('Request failed (HTTP '+r.status+').');
  return r.status===204?null:r.json();
 }
 function row(parent,title,info,action,buttonLabel){
  const div=document.createElement('div');div.className='payment-row';
  const h=document.createElement('strong');h.textContent=title;
  const p=document.createElement('p');p.textContent=info;
  div.append(h,p);
  if(action){const button=document.createElement('button');button.className='secondary-button';button.textContent=buttonLabel;button.onclick=action;div.append(button)}
  parent.append(div);return div;
 }
 async function coupons(){
  const area=document.querySelector('#coupon-admin-list');area.replaceChildren();
  try{
   const items=await api('/api/admin/coupons');
   if(!items.length)area.textContent='No coupons yet.';
   for(const c of items)row(area,c.code,c.percent+'% off · '+c.usedCount+' uses · '+(c.maxUses||'Unlimited')+' maximum · '+(c.active?'Active':'Disabled'),async()=>{
    try{await api('/api/admin/coupons/'+c.id+'/toggle','POST');await coupons()}catch(e){alert(e.message)}
   },c.active?'Disable':'Enable');
  }catch(e){area.textContent=e.message}
 }
 document.querySelector('#coupon-form').onsubmit=async e=>{
  e.preventDefault();const status=document.querySelector('#coupon-admin-message');
  try{
   await api('/api/admin/coupons','POST',{code:document.querySelector('#coupon-name').value.trim(),percent:Number(document.querySelector('#coupon-percent').value),maxUses:Number(document.querySelector('#coupon-limit').value)});
   status.textContent='Coupon created.';e.target.reset();await coupons();
  }catch(err){status.textContent=err.message}
 };
 async function announcements(){
  const area=document.querySelector('#announcement-admin-list');area.replaceChildren();
  try{
   const items=await api('/api/admin/announcements');if(!items.length)area.textContent='No announcements yet.';
   for(const a of items)row(area,a.title,a.message+' · '+(a.active?'Published':'Hidden'),async()=>{
    try{await api('/api/admin/announcements/'+a.id+'/toggle','POST');await announcements()}catch(e){alert(e.message)}
   },a.active?'Hide':'Show');
  }catch(e){area.textContent=e.message}
 }
 document.querySelector('#announcement-form').onsubmit=async e=>{
  e.preventDefault();const status=document.querySelector('#announcement-admin-message');
  try{
   await api('/api/admin/announcements','POST',{title:document.querySelector('#announcement-title').value.trim(),message:document.querySelector('#announcement-message').value.trim()});
   status.textContent='Announcement published.';e.target.reset();await announcements();
  }catch(err){status.textContent=err.message}
 };
 async function reviews(){
  const area=document.querySelector('#review-admin-list');area.replaceChildren();
  try{
   const items=await api('/api/admin/reviews');if(!items.length)area.textContent='No student reviews yet.';
   for(const r of items)row(area,r.course+' · '+r.student+' · '+r.rating+'/5',r.comment+' · '+(r.published?'Published':'Awaiting moderation'),async()=>{
    try{await api('/api/admin/reviews/'+r.id+'/toggle','POST');await reviews()}catch(e){alert(e.message)}
   },r.published?'Unpublish':'Publish');
  }catch(e){area.textContent=e.message}
 }
 async function tickets(){
  const area=document.querySelector('#ticket-admin-list');area.replaceChildren();
  try{
   const items=await api('/api/admin/tickets');if(!items.length)area.textContent='No support tickets yet.';
   for(const t of items){
    const div=row(area,'#'+t.id+' · '+t.subject,t.studentEmail+' · '+t.status+' · '+t.message+(t.adminReply?' · Reply: '+t.adminReply:''));
    const input=document.createElement('textarea');input.rows=2;input.maxLength=3000;input.placeholder='Reply to student (visible in their account)';
    const button=document.createElement('button');button.textContent='Reply & resolve';button.className='admin-button';
    button.onclick=async()=>{
     if(!input.value.trim())return;
     try{await api('/api/admin/tickets/'+t.id+'/reply','POST',{reply:input.value.trim()});await tickets()}catch(e){alert(e.message)}
    };
    div.append(input,button);
   }
  }catch(e){area.textContent=e.message}
 }
 async function visitorStats(){
  const area=document.querySelector('#visitor-analytics');area.replaceChildren();
  try{
   const v=await api('/api/admin/visitors');
   const p=document.createElement('strong');p.textContent='Recorded page views: '+v.pageViews;area.append(p);
   for(const day of v.lastDays)row(area,day.date,day.views+' page views');
   const note=document.createElement('p');note.className='checkout-note';note.textContent='Counts are approximate page views, not unique visitors or sales.';area.append(note);
  }catch(e){area.textContent=e.message}
 }
 await Promise.all([coupons(),announcements(),reviews(),tickets(),visitorStats()]);
})();