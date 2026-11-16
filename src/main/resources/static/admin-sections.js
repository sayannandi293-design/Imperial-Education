(()=>{
 // Turn existing admin sections into accessible click-to-expand topics, without
 // changing their original forms, IDs, or JS event handlers.
 const container=document.querySelector('main.admin-container');
 if(!container)return;
 const sections=Array.from(container.querySelectorAll(':scope > section.admin-panel'));
 const entries=[];
 sections.forEach((section,index)=>{
  const heading=section.querySelector(':scope > h2');
  if(!heading)return;
  const title=heading.textContent.trim();
  const key=title.toLowerCase().replace(/[^a-z0-9]+/g,'-').replace(/^-|-$/g,'')+'-'+index;
  const wrapper=document.createElement('details');
  wrapper.className='admin-topic';
  wrapper.dataset.topic=key;
  const summary=document.createElement('summary');
  summary.className='admin-topic-title';
  const label=document.createElement('span');label.textContent=title;
  const arrow=document.createElement('span');arrow.className='admin-topic-arrow';arrow.setAttribute('aria-hidden','true');arrow.textContent='⌄';
  summary.append(label,arrow);
  section.parentNode.insertBefore(wrapper,section);
  wrapper.append(summary,section);
  heading.classList.add('admin-topic-inner-title');
  entries.push({wrapper,key,title});
  wrapper.addEventListener('toggle',()=>{
   if(!wrapper.open)return;
   entries.forEach(e=>{if(e.wrapper!==wrapper)e.wrapper.open=false});
   history.replaceState(null,'','#admin-'+key);
  });
 });
 function openTopic(key){
  const selected=entries.find(e=>e.key===key||e.title.toLowerCase()===key);
  if(!selected)return;
  entries.forEach(e=>e.wrapper.open=e===selected);
  selected.wrapper.scrollIntoView({block:'start',behavior:'smooth'});
 }
 const initial=decodeURIComponent(location.hash.replace(/^#admin-/,''));
 if(location.hash.startsWith('#admin-')&&initial)openTopic(initial);
 // Editing a course should reveal the form even if it was collapsed.
 const courseList=document.querySelector('#admin-courses');
 if(courseList)courseList.addEventListener('click',e=>{
  if(e.target.closest('button')?.textContent?.trim()==='Edit'){
   const form=document.querySelector('#course-form');
   const parent=form?.closest('details.admin-topic');
   if(parent){entries.forEach(x=>x.wrapper.open=x.wrapper===parent)}
  }
 },true);
 window.addEventListener('hashchange',()=>{
  if(location.hash.startsWith('#admin-'))openTopic(decodeURIComponent(location.hash.slice(7)));
 });
})();