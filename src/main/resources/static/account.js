async function init(){
 const welcome=document.querySelector('#welcome');
 try{
  const [p,t]=await Promise.all([fetch('/api/account/me'),fetch('/api/account/csrf')]);
  if(!p.ok||!t.ok)throw Error('Please sign in again.');
  const profile=await p.json(),csrf=await t.json();
  welcome.textContent='Welcome, '+profile.fullName+' ('+profile.email+')';
  const hidden=document.createElement('input');hidden.type='hidden';hidden.name='_csrf';hidden.value=csrf.token;
  document.querySelector('#logout-form').append(hidden);
  const [cr,pr]=await Promise.all([fetch('/api/student/my-courses'),fetch('/api/student/payments')]);
  if(!cr.ok||!pr.ok)throw Error('Unable to load your courses.');
  const courses=await cr.json(),payments=await pr.json();
  const mine=document.querySelector('#my-courses'),pending=document.querySelector('#my-payments');
  mine.replaceChildren();pending.replaceChildren();
  if(!courses.length)mine.textContent='No approved courses yet. Explore courses to get started.';
  for(const c of courses){
   const row=document.createElement('div');row.className='payment-row';
   const title=document.createElement('strong');title.textContent=c.title;
   const note=document.createElement('p');note.textContent='Enrollment approved.';
   const groupBox=document.createElement('div');groupBox.textContent='Checking Telegram group…';
   row.append(title,note,groupBox);mine.append(row);
   try{
    const response=await fetch('/api/student/courses/'+encodeURIComponent(c.courseId)+'/telegram');
    if(!response.ok)throw Error('Unable to load Telegram group.');
    const group=await response.json();groupBox.replaceChildren();
    if(!group.inviteUrl){groupBox.textContent='Telegram group will be available soon.';continue}
    const parsed=new URL(group.inviteUrl);
    if(parsed.protocol!=='https:'||!['t.me','telegram.me'].includes(parsed.hostname)){groupBox.textContent='Invalid Telegram group link. Contact support.';continue}
    const link=document.createElement('a');link.href=parsed.href;link.target='_blank';link.rel='noopener noreferrer';
    link.textContent='Join Telegram Group';link.className='admin-button telegram-join';
    const hint=document.createElement('p');hint.textContent='If join approval is required, wait for the Telegram group administrator to accept your request.';
    groupBox.append(link,hint);
   }catch(e){groupBox.textContent=e.message}
  }
  if(!payments.length)pending.textContent='No payment requests yet.';
  for(const payment of payments){
   const row=document.createElement('div');row.className='payment-row';
   const title=document.createElement('strong');title.textContent=payment.courseTitle;
   const label=document.createElement('p');label.textContent='₹'+(payment.amountInPaise/100).toFixed(2)+' · Reference '+payment.utr+' · '+payment.status;
   row.append(title,label);pending.append(row);
  }
 }catch(e){welcome.textContent=e.message}
}
init();