(async()=>{
 const area=document.querySelector('#invoice-details');
 const button=document.querySelector('#print-invoice');
 const id=new URLSearchParams(location.search).get('order');
 if(!id||!/^[0-9]{1,12}$/.test(id)){area.textContent='Invalid invoice number.';return}
 try{
  const response=await fetch('/api/student/payments/'+encodeURIComponent(id)+'/invoice',{credentials:'same-origin',cache:'no-store'});
  if(!response.ok)throw Error(response.status===409?'Invoice available after payment approval.':'Could not load this invoice. Sign in to the purchasing student account.');
  const data=await response.json();area.replaceChildren();
  const section=document.createElement('div');section.className='invoice-grid';
  const pairs=[['Invoice / Order number',data.invoiceNumber],['Seller',data.sellerName],['Seller address',data.sellerAddress||'Not provided'],['Seller email',data.sellerEmail||'Not provided'],['Buyer',data.studentName],['Buyer email',data.studentEmail],['Course',data.courseTitle],['Payment reference (UTR)',data.utr],['Submitted',new Date(data.paymentSubmittedAt).toLocaleString('en-IN')],['Approved',new Date(data.approvedAt).toLocaleString('en-IN')]];
  for(const [label,value] of pairs){
   const line=document.createElement('div');const k=document.createElement('span');k.textContent=label;const v=document.createElement('strong');v.textContent=value;line.append(k,v);section.append(line);
  }
  area.append(section);
  const total=document.createElement('div');total.className='invoice-total';
  total.textContent='Amount received: ₹'+(data.amountInPaise/100).toLocaleString('en-IN',{minimumFractionDigits:2,maximumFractionDigits:2});
  const note=document.createElement('p');note.textContent=data.note;
  area.append(total,note);button.hidden=false;
  button.onclick=()=>window.print();
 }catch(e){area.textContent=e.message}
})();