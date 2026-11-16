(()=>{
 const tabs=[...document.querySelectorAll('[data-account-tab]')];
 const panels=[...document.querySelectorAll('[data-account-panel]')];
 const names=tabs.map(t=>t.dataset.accountTab);
 function show(name,updateUrl){
  if(!names.includes(name))name='courses';
  for(const tab of tabs){
   const active=tab.dataset.accountTab===name;
   tab.classList.toggle('active',active);tab.setAttribute('aria-selected',String(active));
   tab.tabIndex=active?0:-1;
  }
  for(const panel of panels)panel.hidden=panel.dataset.accountPanel!==name;
  if(updateUrl)history.replaceState(null,'','#'+name);
 }
 tabs.forEach((tab,i)=>{
  tab.addEventListener('click',()=>show(tab.dataset.accountTab,true));
  tab.addEventListener('keydown',event=>{
   let next=i;
   if(event.key==='ArrowRight'||event.key==='ArrowDown')next=(i+1)%tabs.length;
   else if(event.key==='ArrowLeft'||event.key==='ArrowUp')next=(i+tabs.length-1)%tabs.length;
   else if(event.key==='Home')next=0;
   else if(event.key==='End')next=tabs.length-1;
   else return;
   event.preventDefault();tabs[next].focus();show(tabs[next].dataset.accountTab,true);
  });
 });
 window.addEventListener('hashchange',()=>show(location.hash.slice(1),false));
 show(location.hash.slice(1),false);
})();