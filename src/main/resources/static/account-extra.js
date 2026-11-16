(async()=>{
 const token=await fetch('/api/account/csrf').then(r=>r.ok?r.json():null).catch(()=>null);
 if(!token)return;
 async function api(path,method='GET',body){
  const r=await fetch(path,{method,credentials:'same-origin',headers:{[token.headerName]:token.token,...(body?{'Content-Type':'application/json'}:{})},body:body?JSON.stringify(body):undefined});
  if(!r.ok)throw Error('Request failed (HTTP '+r.status+').');
  return r.status===204?null:r.json();
 }
 function item(area,title,info){
  const row=document.createElement('div');row.className='payment-row';
  const h=document.createElement('strong');h.textContent=title;
  const p=document.createElement('p');p.textContent=info;row.append(h,p);area.append(row);return row;
 }
 async function wishlist(){
  const area=document.querySelector('#student-wishlist');area.replaceChildren();
  try{
   const data=await api('/api/student/wishlist');
   if(!data.length)area.textContent='Your wishlist is empty. Save courses from the homepage.';
   for(const w of data){
    const row=item(area,w.title,'₹'+(w.priceInPaise/100).toLocaleString('en-IN'));
    const link=document.createElement('a');link.href='/checkout.html?course='+encodeURIComponent(w.courseId);link.textContent='Buy course';row.append(link);
    const remove=document.createElement('button');remove.className='secondary-button';remove.textContent='Remove';remove.style.marginLeft='12px';
    remove.onclick=async()=>{try{await api('/api/student/wishlist/'+w.courseId,'DELETE');await wishlist()}catch(e){alert(e.message)}};
    row.append(remove);
   }
  }catch(e){area.textContent=e.message}
 }
 let currentChat=null,chatRefresh=null;
 async function tickets(selectId){
  const area=document.querySelector('#student-tickets');area.replaceChildren();
  if(chatRefresh){clearInterval(chatRefresh);chatRefresh=null}
  try{
   const items=await api('/api/student/tickets');
   if(!items.length){area.textContent='No conversations yet. Create a support ticket above.';return}
   const layout=document.createElement('div');layout.className='support-chat-layout';
   const list=document.createElement('div');list.className='support-chat-list';
   const thread=document.createElement('div');thread.className='support-chat-thread';
   layout.append(list,thread);area.append(layout);
   const buttons=[];
   async function open(ticket){
    currentChat=ticket.id;
    buttons.forEach(b=>b.classList.toggle('selected',Number(b.dataset.id)===currentChat));
    thread.replaceChildren();
    const header=document.createElement('div');header.className='support-chat-header';
    const title=document.createElement('strong');title.textContent='#'+ticket.id+' · '+ticket.subject;
    const small=document.createElement('small');small.textContent='IMPERIAL EDUCATION Support';
    const remove=document.createElement('button');remove.type='button';remove.className='danger-button support-chat-delete';remove.textContent='Delete Chat';
    remove.onclick=async()=>{
     if(!confirm('Permanently delete this ticket and ALL messages for both you and support? This cannot be undone.'))return;
     remove.disabled=true;
     try{
      await api('/api/student/tickets/'+ticket.id,'DELETE');
      if(chatRefresh){clearInterval(chatRefresh);chatRefresh=null}
      currentChat=null;await tickets();
     }catch(e){status.textContent=e.message;remove.disabled=false}
    };
    header.append(title,small,remove);thread.append(header);
    const feed=document.createElement('div');feed.className='support-chat-messages';
    feed.setAttribute('aria-live','polite');thread.append(feed);
    const form=document.createElement('form');form.className='support-chat-compose';
    const input=document.createElement('textarea');input.rows=2;input.maxLength=3000;input.required=true;input.placeholder='Type a message…';
    const send=document.createElement('button');send.className='admin-button';send.type='submit';send.textContent='Send';
    const status=document.createElement('p');status.className='support-chat-status';status.setAttribute('role','status');
    form.append(input,send,status);thread.append(form);
    const load=async(forceScroll=false)=>{
     if(currentChat!==ticket.id)return;
     try{
      const messages=await api('/api/student/tickets/'+ticket.id+'/messages');
      if(currentChat!==ticket.id)return;
      const nearBottom=feed.scrollHeight-feed.scrollTop-feed.clientHeight<110;
      feed.replaceChildren();
      for(const m of messages){
       const bubble=document.createElement('div');bubble.className='support-bubble '+(m.sender==='STUDENT'?'outgoing':'incoming');
       const p=document.createElement('p');p.textContent=m.body;
       const time=document.createElement('small');
       time.textContent=(m.sender==='STUDENT'?'You · ':'Support · ')+new Date(m.createdAt).toLocaleString();
       bubble.append(p,time);feed.append(bubble);
      }
      if(forceScroll||nearBottom)feed.scrollTop=feed.scrollHeight;
     }catch(e){status.textContent=e.message}
    };
    form.onsubmit=async e=>{
     e.preventDefault();const body=input.value.trim();if(!body)return;
     send.disabled=true;status.textContent='';
     try{await api('/api/student/tickets/'+ticket.id+'/messages','POST',{body});input.value='';await load(true)}
     catch(e){status.textContent=e.message}
     finally{send.disabled=false}
    };
    await load(true);
    if(chatRefresh)clearInterval(chatRefresh);
    chatRefresh=setInterval(()=>{if(currentChat===ticket.id&&!document.hidden)load()},6000);
   }
   for(const t of items){
    const button=document.createElement('button');button.type='button';button.className='support-chat-person';button.dataset.id=t.id;
    const title=document.createElement('strong');title.textContent='IMPERIAL EDUCATION Support';
    const subject=document.createElement('span');subject.textContent='#'+t.id+' · '+t.subject;
    const snippet=document.createElement('small');snippet.textContent=t.adminReply||t.message;
    button.append(title,subject,snippet);button.onclick=()=>open(t);list.append(button);buttons.push(button);
   }
   await open(items.find(t=>t.id===selectId)||items[0]);
  }catch(e){area.textContent=e.message}
 }
 document.querySelector('#student-ticket-form').onsubmit=async e=>{
  e.preventDefault();const status=document.querySelector('#student-ticket-message');
  try{
   await api('/api/student/tickets','POST',{subject:document.querySelector('#ticket-subject').value.trim(),message:document.querySelector('#ticket-message').value.trim()});
   status.textContent='Your ticket was sent.';e.target.reset();await tickets();
  }catch(err){status.textContent=err.message}
 };
 async function reviewCourses(){
  const select=document.querySelector('#student-review-course');select.replaceChildren();
  try{
   const courses=await api('/api/student/my-courses');
   if(!courses.length){const o=document.createElement('option');o.value='';o.textContent='No approved courses';select.append(o);return}
   for(const c of courses){const option=document.createElement('option');option.value=c.courseId;option.textContent=c.title;select.append(option)}
  }catch(e){document.querySelector('#student-review-message').textContent=e.message}
 }
 document.querySelector('#student-review-form').onsubmit=async e=>{
  e.preventDefault();const status=document.querySelector('#student-review-message');
  const id=document.querySelector('#student-review-course').value;
  if(!id){status.textContent='An approved course is required.';return}
  try{
   const r=await api('/api/student/courses/'+encodeURIComponent(id)+'/review','POST',{rating:Number(document.querySelector('#student-review-rating').value),comment:document.querySelector('#student-review-comment').value.trim()});
   status.textContent=r.message;e.target.reset();
  }catch(err){status.textContent=err.message}
 };
 async function receipts(){
  const area=document.querySelector('#my-payments');
  if(!area)return;
  try{
   const payments=await api('/api/student/payments');
   const rows=area.querySelectorAll('.payment-row');
   payments.forEach((p,i)=>{
    if(!rows[i])return;
    const button=document.createElement('button');button.type='button';button.className='secondary-button';
    button.textContent='View receipt · IE-'+p.id;
    button.onclick=async()=>{
     try{
      const receipt=await api('/api/student/payments/'+p.id+'/receipt');
      const content=[
       'IMPERIAL EDUCATION - PAYMENT RECORD',
       'Order: '+receipt.orderId,
       'Student: '+receipt.student,
       'Course: '+receipt.course,
       'Amount: INR '+(receipt.amountInPaise/100).toFixed(2),
       'Status: '+receipt.status,
       'UTR: '+receipt.utr,
       'Submitted: '+receipt.submittedAt,
       'Reviewed: '+(receipt.reviewedAt||'Awaiting verification'),
       'Note: This is a payment record, not a tax invoice.'
      ].join('\\n');
      const blob=new Blob([content],{type:'text/plain;charset=utf-8'});
      const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download='imperial-'+receipt.orderId+'.txt';a.click();
      setTimeout(()=>URL.revokeObjectURL(url),1000);
     }catch(e){alert(e.message)}
    };
    rows[i].append(button);
   });
  }catch{}
 }
 await Promise.all([wishlist(),tickets(),reviewCourses()]);
 // Original account.js may still be rendering; retrieve receipt rows after its async requests.
 const ready=setInterval(()=>{
  const area=document.querySelector('#my-payments');
  if(area&&area.querySelector('.payment-row')){clearInterval(ready);receipts()}
 },300);
 setTimeout(()=>clearInterval(ready),9000);
})();