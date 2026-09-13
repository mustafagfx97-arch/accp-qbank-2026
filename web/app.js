'use strict';
let DB=null;
const app=document.getElementById('app');
const store={
  get progress(){return JSON.parse(localStorage.getItem('accp2026_progress')||'{}')},
  set progress(v){localStorage.setItem('accp2026_progress',JSON.stringify(v))},
  get bookmarks(){return new Set(JSON.parse(localStorage.getItem('accp2026_bookmarks')||'[]'))},
  set bookmarks(v){localStorage.setItem('accp2026_bookmarks',JSON.stringify([...v]))}
};
let session=null;

async function loadDB(){
  const r=await fetch('data/questions.json.gz',{cache:'no-cache'});
  if(!r.ok) throw new Error(`Failed to load question bank (${r.status})`);
  let text;
  if('DecompressionStream' in window){
    const ds=new DecompressionStream('gzip');
    text=await new Response(r.body.pipeThrough(ds)).text();
  }else{
    throw new Error('This browser does not support gzip decompression. Please use a current Chrome, Edge, Firefox, or Safari version.');
  }
  return JSON.parse(text);
}
const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const usable=q=>q.ocr_status!=='source_missing';
function chapterQs(id,type='all'){return DB.questions.filter(q=>q.chapter_id===id&&usable(q)&&(type==='all'||q.type===type));}
function solvedCount(id){const p=store.progress;return chapterQs(id).filter(q=>p[q.id]?.answered).length}
function shell(content,title='ACCP QBank 2026'){
  app.innerHTML=`<div class="shell"><header class="topbar"><div class="topbar-inner"><div class="brand" onclick="goHome()" style="cursor:pointer"><div class="logo">A</div><div>${esc(title)}</div></div><div class="top-actions"><button class="iconbtn" onclick="showBookmarks()">🔖 <span class="label">المفضلة</span></button><button class="iconbtn" onclick="goHome()">⌂ <span class="label">الرئيسية</span></button></div></div></header><main class="container">${content}</main></div>`;
}
function goHome(){session=null;location.hash='home';renderHome()}
function renderHome(){
  const p=store.progress, answered=Object.values(p).filter(x=>x?.answered).length, correct=Object.values(p).filter(x=>x?.correct).length;
  const usableN=DB.questions.filter(usable).length, acc=answered?Math.round(correct/answered*100):0;
  const chapters=DB.chapters.map((c,i)=>{const avail=chapterQs(c.id).length,s=solvedCount(c.id),pct=avail?Math.round(s/avail*100):0;return `<div class="chapter-card" onclick="chapterSetup('${c.id}')"><div class="chapter-num">${i+1}</div><div class="chapter-main"><h3>${esc(c.title)}</h3><p>${c.assessment_count} Assessment • ${c.case_count} Case • ${avail} available</p><div class="progress"><i style="width:${pct}%"></i></div><p style="margin-top:5px">تم حل ${s} من ${avail}</p></div></div>`}).join('');
  shell(`<section class="hero"><h1>ACCP QBank 2026</h1><p>نسخة ويب مُعاد بناؤها لعرض نص السؤال كاملًا وربط الـAssessment والـCase Study بسياق الحالة المشترك بدون قص أو خلط.</p><div class="stats"><div class="stat"><b>${usableN}</b><span>سؤال متاح</span></div><div class="stat"><b>${answered}</b><span>تم حلها</span></div><div class="stat"><b>${correct}</b><span>صحيحة</span></div><div class="stat"><b>${acc}%</b><span>الدقة</span></div></div></section>
  <div class="quick-grid"><div class="action-card" onclick="quickStart(10)"><h3>جلسة سريعة</h3><p>10 أسئلة عشوائية من جميع الفصول مع تصحيح وتفسير فوري.</p></div><div class="action-card" onclick="startAll('assessment')"><h3>Assessment فقط</h3><p>كل أسئلة الـSelf‑Assessment بترتيب المصدر مع إظهار أي case context كامل.</p></div><div class="action-card" onclick="startAll('case')"><h3>Case Study فقط</h3><p>الحالات السريرية كما في المصدر، مع تكرار سياق الحالة عند كل سؤال لضمان عدم ضياعه.</p></div></div>
  <div class="section-title"><div><h2>فصول بنك الأسئلة</h2><p>22 فصلاً</p></div><span class="badge">533 مصدرية • 531 متاحة</span></div><div class="chapters">${chapters}</div>`);
}
function chapterSetup(id){
  const c=DB.chapters.find(x=>x.id===id), counts={all:chapterQs(id).length,assessment:chapterQs(id,'assessment').length,case:chapterQs(id,'case').length};
  shell(`<div class="breadcrumb"><button onclick="goHome()">الرئيسية</button><span>←</span><span>الفصل</span></div><div class="setup-card"><div class="setup-head"><h2>${esc(c.title)}</h2><p>اختر نوع الأسئلة. النص الكامل للحالة سيظهر دائمًا قبل السؤال إذا كان مشتركًا مع أسئلة أخرى.</p></div><div class="segmented" id="types"><button class="active" data-type="all">الكل (${counts.all})</button><button data-type="assessment">Assessment (${counts.assessment})</button><button data-type="case">Case Study (${counts.case})</button></div><button class="primary" id="startChapter">ابدأ الفصل</button></div>`);
  let type='all';document.querySelectorAll('#types button').forEach(b=>b.onclick=()=>{document.querySelectorAll('#types button').forEach(x=>x.classList.remove('active'));b.classList.add('active');type=b.dataset.type});document.getElementById('startChapter').onclick=()=>startSession(chapterQs(id,type),`${c.title} — ${type}`);
}
function quickStart(n){const pool=DB.questions.filter(usable);for(let i=pool.length-1;i>0;i--){const j=Math.floor(Math.random()*(i+1));[pool[i],pool[j]]=[pool[j],pool[i]]}startSession(pool.slice(0,n),'Quick 10')}
function startAll(type){startSession(DB.questions.filter(q=>usable(q)&&q.type===type),type==='assessment'?'All Assessments':'All Case Studies')}
function startSession(questions,title){session={questions:[...questions],title,index:0,answers:{},submitted:new Set()};renderQuestion()}
function renderQuestion(){
  if(!session||!session.questions.length)return goHome();
  const q=session.questions[session.index], chosen=session.answers[q.id], submitted=session.submitted.has(q.id), bm=store.bookmarks, isBm=bm.has(q.id), c=DB.chapters.find(x=>x.id===q.chapter_id);
  const opts=['A','B','C','D'].map(k=>{let cls='option';if(chosen===k)cls+=' selected';if(submitted&&k===q.answer)cls+=' correct';if(submitted&&chosen===k&&k!==q.answer)cls+=' incorrect';return `<div class="${cls}" onclick="choose('${k}')"><div class="letter">${k}</div><div>${esc(q.options[k])}</div></div>`}).join('');
  const ctx=q.case_context?`<div class="case-context"><div class="label">سياق الحالة — يظهر كاملاً مع كل سؤال مرتبط بها</div>${esc(q.case_context)}</div>`:'';
  const note=q.ocr_status!=='parsed'?`<div class="source-note">تنبيه المصدر: ${esc((q.ocr_warnings||[]).join(' • ')||q.ocr_status)}</div>`:'';
  const expl=submitted?`<div class="explanation"><b>الإجابة الصحيحة: ${q.answer} — التفسير</b>${esc(q.explanation)}</div>`:'';
  shell(`<div class="breadcrumb"><button onclick="goHome()">الرئيسية</button><span>←</span><span>${esc(c.title)}</span></div><div class="quiz-head"><div class="qmeta"><h2>${esc(session.title)}</h2><p>سؤال ${session.index+1} من ${session.questions.length} • ${q.type==='assessment'?'Assessment':'Case Study'} #${q.number} • Source page ${q.question_page??'—'}</p></div><button class="iconbtn" onclick="toggleBookmark('${q.id}')">${isBm?'★ محفوظ':'☆ حفظ'}</button></div><div class="progress"><i style="width:${Math.round((session.index+1)/session.questions.length*100)}%"></i></div><div style="height:12px"></div><div class="question-card">${ctx}<div class="stem">${esc(q.stem)}</div><div class="options">${opts}</div>${note}${expl}</div><div class="quiz-nav"><div class="right"><button class="secondary" onclick="prevQ()" ${session.index===0?'disabled':''}>السابق</button><button class="secondary" onclick="nextQ()">${session.index===session.questions.length-1?'النتيجة':'التالي'}</button></div>${!submitted?`<button class="primary" onclick="submitQ()" ${!chosen?'disabled':''}>تحقق من الإجابة</button>`:'<span class="badge">تم التصحيح</span>'}</div>`);
}
function choose(k){const q=session.questions[session.index];if(session.submitted.has(q.id))return;session.answers[q.id]=k;renderQuestion()}
function submitQ(){const q=session.questions[session.index],chosen=session.answers[q.id];if(!chosen)return;session.submitted.add(q.id);const p=store.progress;p[q.id]={answered:true,correct:chosen===q.answer,answer:chosen,at:Date.now()};store.progress=p;renderQuestion()}
function prevQ(){if(session.index>0){session.index--;renderQuestion()}}
function nextQ(){if(session.index<session.questions.length-1){session.index++;renderQuestion()}else renderResults()}
function renderResults(){let answered=0,correct=0;const rows=session.questions.map(q=>{const a=session.answers[q.id];if(a){answered++;if(a===q.answer)correct++}return `<div class="review-row ${a===q.answer?'good':'bad'}"><b>${esc(DB.chapters.find(c=>c.id===q.chapter_id)?.title)} — ${q.type} #${q.number}</b><div class="mini">إجابتك: ${esc(a||'—')} • الصحيحة: ${q.answer}</div></div>`}).join('');const pct=answered?Math.round(correct/answered*100):0;shell(`<div class="result-card"><div class="score">${pct}%</div><h2>${correct} صحيحة من ${answered} مجابة</h2><p class="mini">الجلسة: ${esc(session.title)}</p><button class="primary" onclick="goHome()">العودة للرئيسية</button><div class="review-list">${rows}</div></div>`)}
function toggleBookmark(id){const b=store.bookmarks;b.has(id)?b.delete(id):b.add(id);store.bookmarks=b;renderQuestion()}
function showBookmarks(){const b=store.bookmarks, qs=DB.questions.filter(q=>b.has(q.id)&&usable(q));if(!qs.length){shell('<div class="empty"><h2>لا توجد أسئلة محفوظة بعد</h2><p>استخدم زر ☆ أثناء الاختبار لإضافة سؤال للمفضلة.</p><button class="primary" onclick="goHome()">الرئيسية</button></div>');return}startSession(qs,'Bookmarks')}
window.goHome=goHome;window.chapterSetup=chapterSetup;window.quickStart=quickStart;window.startAll=startAll;window.choose=choose;window.submitQ=submitQ;window.prevQ=prevQ;window.nextQ=nextQ;window.toggleBookmark=toggleBookmark;window.showBookmarks=showBookmarks;
loadDB().then(db=>{DB=db;renderHome()}).catch(err=>{app.innerHTML=`<div class="fatal"><h2>Could not load ACCP QBank</h2><p>${esc(err.message)}</p><p>Try reloading in a current browser.</p></div>`});
