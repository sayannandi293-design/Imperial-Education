(()=>{
 const admin=!!document.querySelector('#admin-notification-center');
 const panel=document.querySelector(admin?'#admin-notification-center':'#student-notification-center');
 const badge=document.querySelector(admin?'#admin-notification-count':'#student-notification-count');
 if(!panel||!badge)return;
 const base=admin?'/api/admin/notifications':'/api/student/notifications';
 let busy=false;
 const status=document.createElement('p');status.className='notification-status';status.setAttribute('role','status');
 const bar=document.createElement('div');bar.className='notification-bar';
 const summary=document.createElement('span');
 const refresh=document.createElement('button');refresh.type='button';refresh.className='secondary-button';refresh.textContent='Refresh';
 const items=document.createElement('div');items.className='notification-list';
 bar.append(summary,refresh);
 panel.replaceChildren(bar,items,status);
 function friendlyTime(iso){try{return new Date(iso).toLocaleString('en-IN')}catch{return iso}}
 async function csrfToken(){
  const r=await fetch(admin?'/api/admin/csrf':'/api/account/csrf',{credentials:'same-origin',cache:'no-store'});
  if(!r.ok)throw Error('Please sign in again.');
  return r.json();
 }
 async function markRead(id){
  const t=await csrfToken();
  const r=await fetch(base+'/read',{
   method:'POST',credentials:'same-origin',cache:'no-store',
   headers:{'Content-Type':'application/json',[t.headerName]:t.token},
   body:JSON.stringify({id})
  });
  if(!r.ok)throw Error(r.status===403?'Access denied; please refresh and sign in again.':'Could not mark this notification as read.');
 }
 function safeTarget(value){
  if(typeof value!=='string')return null;
  try{
   const url=new URL(value,location.origin);
   return url.origin===location.origin && (url.pathname==='/admin.html'||url.pathname==='/account.html')?url.pathname+url.hash:null;
  }catch{return null}
 }
 async function load(showErrors=false){
  if(busy)return;
  busy=true;
  try{
   const response=await fetch(base,{credentials:'same-origin',cache:'no-store'});
   if(!response.ok)throw Error('Unable to load notifications. Sign in again.');
   const result=await response.json();
   const unread=Number(result.unreadCount)||0;
   badge.hidden=unread===0;badge.textContent=unread>99?'99+':String(unread);
   summary.textContent=unread+' unread · Showing recent notifications';
   const notices=Array.isArray(result.items)?result.items:[];
   const previouslySelected=document.activeElement?.dataset?.notificationId;
   items.replaceChildren();
   if(!notices.length){items.textContent='No notifications yet.';status.textContent='';return}
   for(const n of notices){
    const article=document.createElement('article');article.className='notification-item'+(n.read?'':' unread');
    const h=document.createElement('strong');h.textContent=n.title;
    const p=document.createElement('p');p.textContent=n.message;
    const time=document.createElement('small');time.textContent=friendlyTime(n.occurredAt);
    const actions=document.createElement('div');actions.className='notification-actions';
    const destination=safeTarget(n.target);
    if(destination){
     const link=document.createElement('a');link.href=destination;
     link.textContent=n.category==='SUPPORT'?'Open Support Chat':n.category==='PAYMENT'?'View Payment / Course':'Open Details';
     link.addEventListener('click',async()=>{
      if(!n.read){try{await markRead(n.id)}catch{}}
     });actions.append(link);
    }
    if(!n.read){
     const button=document.createElement('button');button.type='button';button.className='secondary-button';
     button.textContent='Mark as read';button.dataset.notificationId=n.id;
     button.addEventListener('click',async()=>{
      button.disabled=true;status.textContent='';
      try{await markRead(n.id);await load(true);}
      catch(e){status.textContent=e.message;button.disabled=false}
     });actions.append(button);
    }
    article.append(h,p,time,actions);items.append(article);
   }
   if(previouslySelected)items.querySelectorAll('button').forEach(button=>{if(button.dataset.notificationId===previouslySelected)button.focus()});
   status.textContent='';
  }catch(e){if(showErrors||!items.childElementCount)status.textContent=e.message}
  finally{busy=false}
 }
 refresh.onclick=()=>load(true);
 load(true);
 const timer=setInterval(()=>{if(!document.hidden)load(false)},30000);
 window.addEventListener('pagehide',()=>clearInterval(timer),{once:true});
})();