(async()=>{
 // Keep CSRF protection enabled; validate the session for every admin write.
 async function getCsrf(){
  const r=await fetch('/api/admin/csrf',{credentials:'same-origin',cache:'no-store',redirect:'manual'});
  if(!r.ok||r.type==='opaqueredirect')throw Error('Admin session expired. Sign in again as Admin.');
  return r.json();
 }
 async function currentRole(){
  try{
   const r=await fetch('/api/session',{credentials:'same-origin',cache:'no-store'});
   if(!r.ok)return 'UNKNOWN';
   const s=await r.json();
   return s.signedIn?s.role:'GUEST';
  }catch{return 'UNKNOWN'}
 }
 try{await getCsrf()}catch(e){return}
 async function api(path,method='GET',body){
  const headers={'Accept':'application/json'};
  if(method!=='GET'){
   const csrf=await getCsrf();
   headers[csrf.headerName]=csrf.token;
  }
  if(body)headers['Content-Type']='application/json';
  const r=await fetch(path,{method,credentials:'same-origin',cache:'no-store',redirect:'manual',
   headers,body:body?JSON.stringify(body):undefined});
  if(r.type==='opaqueredirect'||r.status===401)throw Error('Admin session expired. Please log in again.');
  if(r.status===403){
   const role=await currentRole();
   if(role==='STUDENT')throw Error('This browser is currently logged in as STUDENT. Log out and sign in as Admin. Use a separate private window for Student testing.');
   if(role==='GUEST')throw Error('Admin session expired. Please sign in as Admin again.');
   throw Error('HTTP 403 on '+method+' '+path+'. Session role: '+role+'. Please check the matching Render server log; the action was not retried automatically.');
  }
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
   const items=await api('/api/admin/tickets');
   if(!items.length){area.textContent='No support conversations yet.';return}
   const layout=document.createElement('div');layout.className='support-chat-layout';
   const names=document.createElement('div');names.className='support-chat-list';
   const thread=document.createElement('div');thread.className='support-chat-thread';
   layout.append(names,thread);area.append(layout);
   let active=null,refreshId=null,openedTicket=null;
   const buttons=[];
   async function open(ticket){
    active=ticket.id;openedTicket=ticket;
    buttons.forEach(b=>b.classList.toggle('selected',Number(b.dataset.id)===active));
    thread.replaceChildren();
    const header=document.createElement('div');header.className='support-chat-header';
    const title=document.createElement('strong');title.textContent=(ticket.studentName||ticket.studentEmail)+' · #'+ticket.id+' '+ticket.subject;
    const small=document.createElement('small');small.textContent=ticket.studentEmail;
    header.append(title,small);thread.append(header);
    const feed=document.createElement('div');feed.className='support-chat-messages';
    feed.setAttribute('aria-live','polite');thread.append(feed);
    const form=document.createElement('form');form.className='support-chat-compose';
    const input=document.createElement('textarea');input.required=true;input.maxLength=3000;input.rows=2;input.placeholder='Type your message…';
    const send=document.createElement('button');send.className='admin-button';send.textContent='Send';send.type='submit';
    const status=document.createElement('p');status.className='support-chat-status';status.setAttribute('role','status');
    form.append(input,send,status);thread.append(form);
    const renderMessages=async(scroll=false)=>{
     if(active!==ticket.id)return;
     try{
      const messages=await api('/api/admin/tickets/'+ticket.id+'/messages');
      if(active!==ticket.id)return;
      const wasNearBottom=feed.scrollHeight-feed.scrollTop-feed.clientHeight<110;
      feed.replaceChildren();
      for(const m of messages){
       const bubble=document.createElement('div');bubble.className='support-bubble '+(m.sender==='ADMIN'?'outgoing':'incoming');
       const p=document.createElement('p');p.textContent=m.body;
       const time=document.createElement('small');time.textContent=(m.sender==='ADMIN'?'You · ':'Student · ')+new Date(m.createdAt).toLocaleString();
       bubble.append(p,time);feed.append(bubble);
      }
      if(scroll||wasNearBottom)feed.scrollTop=feed.scrollHeight;
     }catch(e){status.textContent=e.message}
    };
    form.onsubmit=async e=>{
     e.preventDefault();const msg=input.value.trim();if(!msg)return;
     send.disabled=true;status.textContent='';
     try{await api('/api/admin/tickets/'+ticket.id+'/messages','POST',{body:msg});input.value='';await renderMessages(true)}
     catch(e){status.textContent=e.message}
     finally{send.disabled=false}
    };
    await renderMessages(true);
    if(refreshId)clearInterval(refreshId);
    refreshId=setInterval(()=>{if(active===ticket.id&&!document.hidden)renderMessages()},6000);
   }
   for(const t of items){
    const b=document.createElement('button');b.type='button';b.className='support-chat-person';b.dataset.id=t.id;
    const name=document.createElement('strong');name.textContent=t.studentName||t.studentEmail;
    const subject=document.createElement('span');subject.textContent='#'+t.id+' · '+t.subject;
    const snippet=document.createElement('small');snippet.textContent=t.adminReply||t.message;
    b.append(name,subject,snippet);b.onclick=()=>open(t);names.append(b);buttons.push(b);
   }
   await open(items[0]);
   area.addEventListener('DOMNodeRemoved',()=>{if(refreshId)clearInterval(refreshId)},{once:true});
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