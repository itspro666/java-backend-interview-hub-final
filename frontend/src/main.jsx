import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import {
  Activity, ArrowRight, ArrowUpRight, Award, BarChart3, Bell, BookOpen,
  Bookmark, BookmarkCheck, BrainCircuit, Check, CheckCircle2, ChevronDown, ChevronLeft,
  ChevronRight, Clock3, Code2, Compass, FileQuestion, Filter,
  Layers, LayoutDashboard, LogIn, LogOut, Menu, Play, Plus, RefreshCw,
  Search, ShieldCheck, Sparkles, Target, Timer, Trophy, User, Users, X, Zap
} from 'lucide-react';
import './style.css';

const API = import.meta.env.VITE_API_URL || '/api';
const emptyQuestion = { title:'', technology:'Java', category:'Core Java', difficulty:'Beginner', explanation:'', codeExample:'', optionA:'', optionB:'', optionC:'', optionD:'', correctOption:'A' };

function readSession() {
  try { return JSON.parse(sessionStorage.getItem('backendhub.session') || 'null'); }
  catch { return null; }
}

function App() {
  const [session, setSession] = useState(readSession);
  const [view, setView] = useState('overview');
  const [authMode, setAuthMode] = useState('login');
  const [authForm, setAuthForm] = useState({ fullName:'', email:'', password:'' });
  const [authBusy, setAuthBusy] = useState(false);
  const [passwordForm, setPasswordForm] = useState({ currentPassword:'', newPassword:'' });
  const [passwordBusy, setPasswordBusy] = useState(false);
  const [toast, setToast] = useState('');
  const [mobileNav, setMobileNav] = useState(false);
  const [questions, setQuestions] = useState([]);
  const [filterOptions, setFilterOptions] = useState({ technologies:[], categories:[] });
  const [questionQuery, setQuestionQuery] = useState('');
  const [technology, setTechnology] = useState('All technologies');
  const [category, setCategory] = useState('All categories');
  const [difficulty, setDifficulty] = useState('All levels');
  const [selectedQuestion, setSelectedQuestion] = useState(null);
  const [bookmarks, setBookmarks] = useState([]);
  const [analytics, setAnalytics] = useState(null);
  const [attempts, setAttempts] = useState([]);
  const [adminOverview, setAdminOverview] = useState(null);
  const [questionForm, setQuestionForm] = useState(emptyQuestion);
  const [editingId, setEditingId] = useState(null);
  const [savingQuestion, setSavingQuestion] = useState(false);
  const [bulkJson, setBulkJson] = useState('');
  const [bulkBusy, setBulkBusy] = useState(false);
  const [examConfig, setExamConfig] = useState({ technology:'', difficulty:'', count:10, durationMinutes:20 });
  const [exam, setExam] = useState(null);
  const [answers, setAnswers] = useState({});
  const [activeExamIndex, setActiveExamIndex] = useState(0);
  const [secondsLeft, setSecondsLeft] = useState(0);
  const [examResult, setExamResult] = useState(null);
  const [examBusy, setExamBusy] = useState(false);
  const submitLock = useRef(false);
  const expirySubmitTriggered = useRef(false);
  const answerSaveQueue = useRef(Promise.resolve());
  const searchInputRef = useRef(null);
  const questionFetchId = useRef(0);

  const token = session?.accessToken;
  const isAdmin = session?.user?.role === 'ADMIN';
  const notify = useCallback((message) => { setToast(message); window.setTimeout(() => setToast(''), 3200); }, []);

  const api = useCallback(async (path, options = {}) => {
    const headers = { ...(options.body ? { 'Content-Type':'application/json' } : {}), ...(token ? { Authorization:`Bearer ${token}` } : {}), ...(options.headers || {}) };
    const response = await fetch(`${API}${path}`, { ...options, headers });
    if (response.status === 204) return null;
    const text = await response.text();
    let data = null;
    try { data = text ? JSON.parse(text) : null; } catch { data = text; }
    if (!response.ok) {
      if (response.status === 401 && token) signOut(false);
      const detail = data?.message || data?.detail || data?.error || `Request failed (${response.status})`;
      const error = new Error(detail); error.status = response.status; throw error;
    }
    return data;
  }, [token]);

  const loadFilterOptions = useCallback(async () => {
    const params = new URLSearchParams({ page:'0', size:'50' });
    const first = await api(`/questions?${params}`);
    const pages = Math.max(1, Number(first?.totalPages || 1));
    const remaining = await Promise.all(Array.from({length:pages-1}, async (_, index) => {
      const next = new URLSearchParams(params); next.set('page', String(index+1));
      const result = await api(`/questions?${next}`);
      return result?.content || [];
    }));
    const rows = [...(first?.content || []), ...remaining.flat()];
    setFilterOptions({
      technologies:[...new Set(rows.map(q => q.technology).filter(Boolean))].sort(),
      categories:[...new Set(rows.map(q => q.category).filter(Boolean))].sort()
    });
  }, [api]);

  const loadQuestions = useCallback(async () => {
    const requestId = ++questionFetchId.current;
    const params = new URLSearchParams({ page:'0', size:'50' });
    if (questionQuery.trim()) params.set('q', questionQuery.trim());
    if (technology !== 'All technologies') params.set('technology', technology);
    if (category !== 'All categories') params.set('category', category);
    if (difficulty !== 'All levels') params.set('difficulty', difficulty);
    const firstPage = await api(`/questions?${params}`);
    const pages = Math.max(1, Number(firstPage?.totalPages || 1));
    const otherPages = await Promise.all(Array.from({length:pages-1}, async (_, index) => {
      const next = new URLSearchParams(params); next.set('page', String(index+1));
      const result = await api(`/questions?${next}`);
      return result?.content || [];
    }));
    if (requestId === questionFetchId.current) setQuestions([...(firstPage?.content || []), ...otherPages.flat()]);
  }, [api, questionQuery, technology, category, difficulty]);

  const loadPrivateData = useCallback(async () => {
    if (!token) { setBookmarks([]); setAnalytics(null); setAttempts([]); setAdminOverview(null); return; }
    try {
      const [saved, history] = await Promise.all([api('/bookmarks'), api('/practice/attempts')]);
      const stats = await api('/analytics/me');
      setBookmarks(saved || []); setAnalytics(stats); setAttempts(history || []);
    } catch (error) { notify(error.message); }
    if (session?.user?.role === 'ADMIN') {
      try { setAdminOverview(await api('/admin/overview')); } catch (error) { notify(error.message); }
    }
  }, [token, session?.user?.role, api, notify]);

  useEffect(() => { const id=window.setTimeout(() => loadQuestions().catch(error => notify(error.message)), 250); return () => window.clearTimeout(id); }, [loadQuestions, notify]);
  useEffect(() => { loadPrivateData(); }, [loadPrivateData]);
  useEffect(() => { loadFilterOptions().catch(error => notify(error.message)); }, [loadFilterOptions, notify]);

  useEffect(() => {
    const onShortcut = (event) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault();
        setView('explore');
        setMobileNav(false);
        window.setTimeout(() => searchInputRef.current?.focus(), 0);
      }
    };
    window.addEventListener('keydown', onShortcut);
    return () => window.removeEventListener('keydown', onShortcut);
  }, []);

  const technologies = useMemo(() => ['All technologies', ...filterOptions.technologies], [filterOptions]);
  const categories = useMemo(() => ['All categories', ...filterOptions.categories], [filterOptions]);
  const savedIds = useMemo(() => new Set(bookmarks.map(b => b.question?.id)), [bookmarks]);
  const average = analytics?.averagePercentage ?? 0;
  const lastAttempt = attempts.find(a => a.status === 'COMPLETED');

  function signOut(showMessage = true) {
    sessionStorage.removeItem('backendhub.session'); setSession(null); setBookmarks([]); setAnalytics(null); setAttempts([]); setAdminOverview(null); setExam(null); setExamResult(null); setView('overview');
    if (showMessage) notify('You have signed out.');
  }

  async function submitAuth(event) {
    event.preventDefault(); setAuthBusy(true);
    try {
      const endpoint = authMode === 'register' ? '/auth/register' : '/auth/login';
      const payload = authMode === 'register' ? authForm : { email:authForm.email, password:authForm.password };
      const result = await fetch(`${API}${endpoint}`, { method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(payload) });
      const data = await result.json();
      if (!result.ok) throw new Error(data?.message || data?.detail || data?.error || 'Authentication failed');
      sessionStorage.setItem('backendhub.session', JSON.stringify(data)); setSession(data); setView('overview'); setAuthForm({fullName:'',email:'',password:''}); notify(`Welcome${data.user?.fullName ? `, ${data.user.fullName}` : ''}.`);
    } catch (error) { notify(error.message); }
    finally { setAuthBusy(false); }
  }

  async function openQuestion(question) {
    try { setSelectedQuestion(await api(`/questions/${question.id}`)); }
    catch (error) { setSelectedQuestion(question); notify(error.message); }
  }

  async function toggleBookmark(questionId) {
    if (!token) { setAuthMode('login'); setView('auth'); notify('Sign in to save questions to your account.'); return; }
    try {
      if (savedIds.has(questionId)) await api(`/bookmarks/${questionId}`, { method:'DELETE' });
      else await api(`/bookmarks/${questionId}`, { method:'POST' });
      const rows = await api('/bookmarks'); setBookmarks(rows || []);
      notify(savedIds.has(questionId) ? 'Removed from saved questions.' : 'Question saved to your library.');
    } catch (error) { notify(error.message); }
  }

  function openPractice() {
    if (!token) { setAuthMode('login'); setView('auth'); notify('Sign in to start a timed mock exam.'); return; }
    setExam(null); setExamResult(null); setView('practice');
  }

  async function startExam(event) {
    event.preventDefault(); setExamBusy(true); submitLock.current = false; expirySubmitTriggered.current = false; answerSaveQueue.current = Promise.resolve();
    try {
      const p = new URLSearchParams({ count:String(examConfig.count), durationMinutes:String(examConfig.durationMinutes) });
      if (examConfig.technology) p.set('technology', examConfig.technology);
      if (examConfig.difficulty) p.set('difficulty', examConfig.difficulty);
      const result = await api(`/practice/start?${p}`, { method:'POST' });
      setExam(result); setAnswers({}); setActiveExamIndex(0); setExamResult(null); setSecondsLeft(Math.max(0, Math.floor((new Date(result.deadlineAt).getTime()-Date.now())/1000)));
    } catch (error) { notify(error.message); }
    finally { setExamBusy(false); }
  }

  async function resumeExam(attemptId) {
    if (!token) { setAuthMode('login'); setView('auth'); return; }
    setExamBusy(true); submitLock.current = false; expirySubmitTriggered.current = false; answerSaveQueue.current = Promise.resolve();
    try {
      const result = await api(`/practice/${attemptId}`);
      const saved = result.savedAnswers || {};
      const firstUnanswered = result.questions.findIndex(question => !saved[question.id]);
      setExam(result); setAnswers(saved); setActiveExamIndex(firstUnanswered < 0 ? 0 : firstUnanswered);
      setExamResult(null); setSecondsLeft(Math.max(0, Math.ceil((new Date(result.deadlineAt).getTime()-Date.now())/1000)));
      setView('practice'); setMobileNav(false); notify('Your saved exam session has been restored.');
    } catch (error) { notify(error.message); await loadPrivateData(); }
    finally { setExamBusy(false); }
  }

  function chooseExamAnswer(questionId, optionKey) {
    if (!exam) return;
    const next = { ...answers, [questionId]:optionKey }; setAnswers(next);
    answerSaveQueue.current = answerSaveQueue.current.catch(() => {}).then(() => api(`/practice/${exam.attemptId}/answers`, { method:'PUT', body:JSON.stringify(next) })).catch(error => notify(error.message));
  }

  const submitExam = useCallback(async (automatic = false) => {
    if (!exam || submitLock.current) return;
    submitLock.current = true; setExamBusy(true);
    try {
      await answerSaveQueue.current.catch(() => {});
      const result = await api(`/practice/${exam.attemptId}/submit`, { method:'POST' });
      setExamResult(result); setExam(null); setView('practice');
      const [stats, history] = await Promise.all([api('/analytics/me'), api('/practice/attempts')]); setAnalytics(stats); setAttempts(history || []);
      notify(automatic ? 'Time is up. Your exam was submitted.' : 'Your exam has been submitted.');
    } catch (error) {
      if (error.status === 409 && exam?.attemptId) {
        try {
          const savedReview = await api(`/practice/attempts/${exam.attemptId}`);
          setExamResult(savedReview); setExam(null); setView('practice');
          const [stats, history] = await Promise.all([api('/analytics/me'), api('/practice/attempts')]);
          setAnalytics(stats); setAttempts(history || []);
          notify('This exam was already submitted. Its saved result is shown below.');
        } catch (reviewError) { submitLock.current = false; notify(reviewError.message); }
      } else { submitLock.current = false; notify(error.message); }
    }
    finally { setExamBusy(false); }
  }, [exam, api, notify]);

  useEffect(() => {
    if (!exam || examResult) return undefined;
    const tick = () => {
      const remaining = Math.max(0, Math.ceil((new Date(exam.deadlineAt).getTime()-Date.now())/1000));
      setSecondsLeft(remaining);
      if (remaining <= 0 && !expirySubmitTriggered.current) { expirySubmitTriggered.current = true; submitExam(true); }
    };
    tick(); const id = window.setInterval(tick, 1000); return () => window.clearInterval(id);
  }, [exam, examResult, submitExam]);

  async function openAttemptReview(attemptId) {
    try { const result=await api(`/practice/attempts/${attemptId}`); setExam(null); setExamResult(result); setView('practice'); }
    catch (error) { notify(error.message); }
  }

  async function changePassword(event) {
    event.preventDefault(); setPasswordBusy(true);
    try { await api('/users/me/password', { method:'PUT', body:JSON.stringify(passwordForm) }); setPasswordForm({currentPassword:'',newPassword:''}); notify('Password updated successfully.'); }
    catch (error) { notify(error.message); }
    finally { setPasswordBusy(false); }
  }

  async function saveQuestion(event) {
    event.preventDefault(); setSavingQuestion(true);
    try {
      await api(editingId ? `/questions/${editingId}` : '/questions', { method:editingId?'PUT':'POST', body:JSON.stringify(questionForm) });
      setQuestionForm(emptyQuestion); setEditingId(null); await loadQuestions(); await loadFilterOptions();
      setAdminOverview(await api('/admin/overview')); notify(editingId ? 'Question updated.' : 'Question published to the bank.');
    } catch (error) { notify(error.message); }
    finally { setSavingQuestion(false); }
  }

  async function importBulkQuestions(event) {
    event.preventDefault(); setBulkBusy(true);
    try { const parsed=JSON.parse(bulkJson); if (!Array.isArray(parsed) || parsed.length<1 || parsed.length>100) throw new Error('Paste a JSON array containing 1 to 100 questions.'); await api('/admin/questions/bulk',{method:'POST',body:JSON.stringify({questions:parsed})}); setBulkJson(''); await loadQuestions(); await loadFilterOptions(); setAdminOverview(await api('/admin/overview')); notify(`${parsed.length} questions imported.`); }
    catch (error) { notify(error instanceof SyntaxError ? 'The JSON is invalid. Check the array syntax.' : error.message); }
    finally { setBulkBusy(false); }
  }

  async function archiveQuestion(id) {
    if (!window.confirm('Archive this question? It will be hidden from learners but existing attempts will remain.')) return;
    try { await api(`/questions/${id}`, { method:'DELETE' }); setSelectedQuestion(null); await loadQuestions(); setAdminOverview(await api('/admin/overview')); notify('Question archived.'); }
    catch (error) { notify(error.message); }
  }

  async function editQuestion(q) {
    try { const full = await api(`/admin/questions/${q.id}`); setEditingId(q.id); setQuestionForm({ title:full.title, technology:full.technology, category:full.category, difficulty:full.difficulty, explanation:full.explanation, codeExample:full.codeExample || '', optionA:full.options?.A || '', optionB:full.options?.B || '', optionC:full.options?.C || '', optionD:full.options?.D || '', correctOption:full.correctOption || 'A' }); setView('admin'); window.scrollTo({ top:0, behavior:'smooth' }); } catch (error) { notify(error.message); }
  }

  const nav = [
    { id:'overview', label:'Overview', icon:LayoutDashboard, group:'Workspace' },
    { id:'account', label:'Account settings', icon:User, group:'Workspace', protected:true },
    { id:'explore', label:'Explore questions', icon:Compass, group:'Workspace' },
    { id:'practice', label:'Mock exams', icon:Target, group:'Practice' },
    { id:'bookmarks', label:'Saved library', icon:Bookmark, group:'Practice', protected:true },
    { id:'analytics', label:'My analytics', icon:BarChart3, group:'Practice', protected:true },
    ...(isAdmin ? [{ id:'admin', label:'Admin studio', icon:ShieldCheck, group:'Manage' }] : [])
  ];

  function navigate(id) {
    if (['bookmarks','analytics','practice','account'].includes(id) && !token) { setAuthMode('login'); setView('auth'); notify('Sign in to access your learning workspace.'); return; }
    setMobileNav(false); setView(id); setSelectedQuestion(null); if (id === 'practice') { setExam(null); setExamResult(null); }
  }

  const activeExamQuestion = exam?.questions?.[activeExamIndex];
  const timerText = `${String(Math.floor(secondsLeft/60)).padStart(2,'0')}:${String(secondsLeft%60).padStart(2,'0')}`;

  return <div className="app-shell">
    <aside className={`sidebar ${mobileNav?'sidebar-open':''}`}>
      <div className="brand-lockup"><div className="brand-mark"><Code2 size={22}/><i/></div><div><b>Backend<span>Hub</span></b><small>ENGINEERING YOUR EDGE</small></div><button className="mobile-close" onClick={()=>setMobileNav(false)} aria-label="Close menu"><X size={18}/></button></div>
      <div className="workspace-pill"><span className="online-dot"/> LEARNING WORKSPACE <ChevronDown size={14}/></div>
      {['Workspace','Practice','Manage'].map(group => {
        const items=nav.filter(item=>item.group===group); if(!items.length)return null;
        return <div className="nav-group" key={group}><div className="nav-caption">{group}</div>{items.map(item=><button key={item.id} onClick={()=>navigate(item.id)} className={`nav-item ${view===item.id?'active':''}`}><item.icon size={17}/><span>{item.label}</span>{item.id==='practice'&&<em>NEW</em>}</button>)}</div>;
      })}
      <div className="sidebar-bottom"><div className="side-promo"><div className="promo-glow"><Sparkles size={18}/></div><b>Level up your career</b><p>Build a little momentum every day. Your next opportunity starts with practice.</p><button onClick={()=>navigate('practice')}>Start a session <ArrowUpRight size={14}/></button></div>
        <div className="account-row">{session ? <><div className="avatar">{(session.user?.fullName||'U').slice(0,1).toUpperCase()}</div><div className="account-info"><b>{session.user?.fullName || 'Learner'}</b><small>{session.user?.role==='ADMIN'?'Platform administrator':'Member account'}</small></div><button className="icon-button" title="Sign out" onClick={()=>signOut()}><LogOut size={16}/></button></> : <><div className="avatar guest-avatar"><User size={17}/></div><div className="account-info"><b>Guest workspace</b><small>Browse the question bank</small></div><button className="icon-button" onClick={()=>{setAuthMode('login');setView('auth')}}><LogIn size={16}/></button></>}</div>
      </div>
    </aside>

    <main className="main-shell">
      <div className="topbar"><button className="mobile-menu" onClick={()=>setMobileNav(true)} aria-label="Open menu"><Menu/></button><div className="breadcrumb"><span>WORKSPACE</span><ChevronRight size={14}/><b>{nav.find(n=>n.id===view)?.label || (view==='auth'?'Account':view==='practice'?'Mock exams':'Learning')}</b></div><div className="topbar-actions"><div className="system-status"><span/> ALL SYSTEMS NOMINAL</div><div className="top-icon" aria-hidden="true"><Bell size={17}/><i/></div><div className="top-avatar">{session?.user?.fullName?.slice(0,1).toUpperCase() || 'G'}</div></div></div>

      {view==='auth' && <section className="auth-layout"><div className="auth-story"><div className="eyebrow"><Sparkles size={14}/> YOUR NEXT CHAPTER STARTS HERE</div><h1>Build the skills.<br/><span>Own the interview.</span></h1><p>A focused workspace for Java backend engineers. Learn the concepts, practice under pressure and track your progress.</p><div className="auth-proof"><div><b>50+</b><small>MCQ PRACTICE ITEMS</small></div><div><b>10</b><small>BACKEND TECHNOLOGIES</small></div><div><b>01</b><small>CONSISTENT HABIT</small></div></div></div><form className="auth-card" onSubmit={submitAuth}><div className="auth-icon"><ShieldCheck size={22}/></div><div className="eyebrow">SECURE ACCESS</div><h2>{authMode==='register'?'Create your account':'Welcome back'}</h2><p>{authMode==='register'?'Create a learner profile to save your progress.':'Sign in to continue your learning journey.'}</p>{authMode==='register'&&<label>Full name<input autoComplete="name" required maxLength="120" value={authForm.fullName} onChange={e=>setAuthForm({...authForm,fullName:e.target.value})} placeholder="Your name"/></label>}<label>Email address<input type="email" autoComplete="email" required value={authForm.email} onChange={e=>setAuthForm({...authForm,email:e.target.value})} placeholder="you@example.com"/></label><label>Password<input type="password" autoComplete={authMode==='register'?'new-password':'current-password'} required minLength={authMode==='register'?8:1} value={authForm.password} onChange={e=>setAuthForm({...authForm,password:e.target.value})} placeholder={authMode==='register'?'At least 8 characters':'Your password'}/></label><button className="primary-button wide" disabled={authBusy}>{authBusy?'Please wait…':authMode==='register'?'Create learner account':'Sign in'} <ArrowRight size={16}/></button><div className="auth-switch">{authMode==='register'?'Already have an account?':'New to BackendHub?'} <button type="button" onClick={()=>setAuthMode(authMode==='register'?'login':'register')}>{authMode==='register'?'Sign in':'Create account'}</button></div><button className="text-button center" type="button" onClick={()=>setView('overview')}>Continue browsing questions</button></form></section>}

      {view==='overview' && <>
        <section className="welcome-row"><div><div className="eyebrow"><span className="eyebrow-line"/> YOUR JAVA CAREER TOOLKIT</div><h1>{session?.user?.fullName ? `Welcome back, ${session.user.fullName.split(' ')[0]}.` : 'Engineer your next opportunity.'}</h1><p>Build fluency in Java backend engineering with focused learning and realistic interview practice.</p></div><div className="welcome-actions"><div className="date-chip"><div className="live-pulse"/> PERSONAL LEARNING HUB</div><button className="primary-button" onClick={()=>navigate('explore')}>Explore question bank <ArrowUpRight size={16}/></button></div></section>
        <section className="hero-panel"><div className="hero-copy"><div className="hero-label"><Zap size={14}/> THE DAILY EDGE</div><h2>Great engineers are<br/><span>built through repetition.</span></h2><p>Turn what you know into what you can confidently explain. A short practice session today is progress you can measure tomorrow.</p><div className="hero-cta"><button className="primary-button" onClick={openPractice}><Play size={15} fill="currentColor"/> Start mock exam</button><button className="ghost-button" onClick={()=>navigate('explore')}>Browse topics <ArrowRight size={15}/></button></div><div className="hero-foot"><span><CheckCircle2 size={14}/> No pressure, just progress</span><span><ShieldCheck size={14}/> Your results stay private</span></div></div><div className="hero-visual"><div className="orb orb-one"/><div className="orb orb-two"/><div className="orbit orbit-a"/><div className="orbit orbit-b"/><div className="visual-core"><BrainCircuit size={44}/><span>JAVA</span></div><div className="float-chip chip-a"><Code2 size={15}/> SPRING BOOT</div><div className="float-chip chip-b"><DatabaseIcon/> DATA & SYSTEMS</div><div className="float-chip chip-c"><Activity size={15}/> PRACTICE MODE</div></div></section>
        <section className="metric-grid"><Metric icon={BookOpen} label="QUESTIONS AVAILABLE" value={questions.length} detail="Knowledge library"/><Metric icon={BookmarkCheck} label="SAVED FOR LATER" value={analytics?.bookmarks ?? (session?bookmarks.length:'—')} detail="Your personal collection"/><Metric icon={Target} label="MOCK EXAMS TAKEN" value={analytics?.totalAttempts ?? '—'} detail="Completed sessions"/><Metric icon={Trophy} label="AVERAGE SCORE" value={analytics?`${Math.round(average)}%`:'—'} detail={analytics?`Best: ${Math.round(analytics.bestPercentage)}%`:'Start an exam to track it'}/></section>
        <section className="dashboard-columns"><div className="panel focus-panel"><div className="panel-heading"><div><div className="eyebrow">YOUR LEARNING PATH</div><h3>Choose your next move</h3></div><span className="panel-icon"><Compass size={18}/></span></div><div className="action-list"><ActionRow icon={FileQuestion} title="Explore the question bank" desc="Understand the concepts behind everyday backend systems." action="Explore" onClick={()=>navigate('explore')}/><ActionRow icon={Timer} title="Take a timed mock exam" desc="Test your recall in a focused, exam-style session." action="Practice" onClick={openPractice}/><ActionRow icon={BarChart3} title="Review your performance" desc="Find strong areas and topics that deserve another look." action="View insights" onClick={()=>navigate('analytics')}/></div></div><div className="panel progress-panel"><div className="panel-heading"><div><div className="eyebrow">PERFORMANCE SNAPSHOT</div><h3>Your momentum</h3></div><span className="panel-icon"><Activity size={18}/></span></div>{analytics ? <><div className="score-ring" style={{'--score':`${Math.min(100,average)}%`}}><div><b>{Math.round(average)}<small>%</small></b><span>AVERAGE</span></div></div><div className="snapshot-stats"><div><b>{analytics.totalExamQuestions}</b><small>Questions in completed exams</small></div><div><b>{Math.round(analytics.bestPercentage)}%</b><small>Personal best</small></div></div><button className="inline-link" onClick={()=>navigate('analytics')}>View full analytics <ArrowRight size={14}/></button></> : <div className="no-data"><div className="no-data-icon"><BarChart3/></div><b>Your first insight is one exam away.</b><p>Complete a mock exam and this panel will begin tracking your momentum.</p><button className="inline-link" onClick={openPractice}>Start your first exam <ArrowRight size={14}/></button></div>}</div></section>
        <section className="panel recent-panel"><div className="panel-heading"><div><div className="eyebrow">RECENT ACTIVITY</div><h3>Keep your learning in motion</h3></div><button className="inline-link" onClick={()=>navigate('analytics')}>All attempts <ArrowRight size={14}/></button></div>{attempts.length ? <div className="attempt-table">{attempts.slice(0,4).map(a=><AttemptRow key={a.id} attempt={a} onClick={()=>a.status==='COMPLETED'?openAttemptReview(a.id):a.status==='IN_PROGRESS'?resumeExam(a.id):undefined}/>)}</div> : <div className="empty-inline"><Clock3 size={18}/><span>No completed exams yet. Your first attempt will appear here.</span></div>}</section>
      </>}

      {view==='explore' && <section className="page-section"><PageTitle eyebrow="KNOWLEDGE BASE" title="Explore the question bank" desc="A curated collection of Java backend concepts, from language fundamentals to distributed systems."/><div className="explore-layout"><div className="explore-main"><div className="search-tools"><div className="search-field"><Search size={17}/><input ref={searchInputRef} value={questionQuery} onChange={e=>setQuestionQuery(e.target.value)} placeholder="Search concepts, technologies or keywords…"/><kbd>⌘ / Ctrl K</kbd></div><div className="filter-row"><label><Filter size={14}/><select value={technology} onChange={e=>setTechnology(e.target.value)}>{technologies.map(t=><option key={t}>{t}</option>)}</select></label><label><select value={category} onChange={e=>setCategory(e.target.value)}>{categories.map(c=><option key={c}>{c}</option>)}</select></label><label><select value={difficulty} onChange={e=>setDifficulty(e.target.value)}><option>All levels</option><option>Beginner</option><option>Intermediate</option><option>Advanced</option></select></label><span className="results-label">{questions.length} RESULTS</span></div></div>{questions.length ? <div className="question-grid">{questions.map((q,index)=><QuestionCard key={q.id} question={q} index={index} saved={savedIds.has(q.id)} selected={selectedQuestion?.id===q.id} onSelect={()=>openQuestion(q)} onSave={()=>toggleBookmark(q.id)} onEdit={isAdmin?()=>editQuestion(q):null}/>)}</div> : <EmptyState icon={Search} title="No matching questions" text="Try adjusting your search or clearing one of the filters."/>}</div><aside className="detail-panel">{selectedQuestion ? <><div className="detail-top"><span className="tag">{selectedQuestion.technology}</span><button className="icon-button" onClick={()=>setSelectedQuestion(null)}><X size={17}/></button></div><div className="detail-meta"><Difficulty value={selectedQuestion.difficulty}/><span>{selectedQuestion.category}</span></div><h2>{selectedQuestion.title}</h2><div className="detail-divider"/><div className="detail-label">CONCEPT BREAKDOWN</div><p className="detail-explanation">{selectedQuestion.explanation}</p>{selectedQuestion.codeExample&&<><div className="detail-label"><Code2 size={14}/> CODE EXAMPLE</div><pre className="code-block"><code>{selectedQuestion.codeExample}</code></pre></>}<div className="detail-actions"><button className="primary-button" onClick={()=>toggleBookmark(selectedQuestion.id)}>{savedIds.has(selectedQuestion.id)?<BookmarkCheck size={16}/>:<Bookmark size={16}/>} {savedIds.has(selectedQuestion.id)?'Saved to library':'Save question'}</button>{isAdmin&&<button className="ghost-button" onClick={()=>editQuestion(selectedQuestion)}>Edit</button>}</div></> : <div className="detail-empty"><div className="detail-empty-icon"><BookOpen size={24}/></div><div className="eyebrow">LEARNING MODE</div><h3>Select a concept</h3><p>Choose a question from the library to see its explanation and code example here.</p><div className="interview-note"><Sparkles size={16}/><div><b>Interview mindset</b><p>Explain the why, not just the what. Connect each concept to a real production scenario.</p></div></div></div>}</aside></div></section>}

      {view==='practice' && <section className="page-section"><PageTitle eyebrow="ASSESSMENT STUDIO" title={exam?'Stay focused. You’ve got this.':'Practice under real conditions.'} desc={exam?'Work through each question at your own pace. Your answers are scored when you submit.':'Build confidence with a timed mock exam. Questions and answer keys are selected and scored by the server.'}/>
        {exam && activeExamQuestion ? <div className="exam-shell"><div className="exam-toolbar"><div><span className="exam-chip"><Target size={14}/> MOCK EXAM</span><b>{exam.technology} · {exam.difficulty}</b></div><div className={`exam-clock ${secondsLeft<60?'urgent':''}`}><Clock3 size={17}/><div><small>TIME REMAINING</small><b>{timerText}</b></div></div></div><div className="exam-progress"><div><span>QUESTION {activeExamIndex+1} OF {exam.questions.length}</span><b>{Math.round(((activeExamIndex+1)/exam.questions.length)*100)}%</b></div><i><span style={{width:`${((activeExamIndex+1)/exam.questions.length)*100}%`}}/></i></div><div className="exam-body"><div className="exam-question"><div className="question-kicker">QUESTION {String(activeExamIndex+1).padStart(2,'0')} <span/> {activeExamQuestion.category}</div><h2>{activeExamQuestion.title}</h2><p>Select the one best answer.</p><div className="option-list">{activeExamQuestion.options.map(option=><button key={option.key} disabled={examBusy} onClick={()=>chooseExamAnswer(activeExamQuestion.id,option.key)} className={`option-row ${answers[activeExamQuestion.id]===option.key?'selected':''}`}><span className="option-key">{option.key}</span><span>{option.text}</span>{answers[activeExamQuestion.id]===option.key&&<Check size={17}/>}</button>)}</div></div><aside className="exam-question-nav"><div className="eyebrow">QUESTION MAP</div><div className="exam-number-grid">{exam.questions.map((q,i)=><button key={q.id} className={`${i===activeExamIndex?'current':''} ${answers[q.id]?'answered':''}`} onClick={()=>setActiveExamIndex(i)}>{i+1}</button>)}</div><div className="question-map-legend"><span><i className="answered-dot"/> Answered</span><span><i/> Unanswered</span></div><div className="exam-answered-count"><b>{Object.keys(answers).length}</b><span>of {exam.questions.length} answered</span></div></aside></div><div className="exam-footer"><button className="ghost-button" disabled={activeExamIndex===0} onClick={()=>setActiveExamIndex(i=>Math.max(0,i-1))}><ChevronLeft size={16}/> Previous</button><span>{Object.keys(answers).length} answers saved</span>{activeExamIndex<exam.questions.length-1?<button className="primary-button" onClick={()=>setActiveExamIndex(i=>Math.min(exam.questions.length-1,i+1))}>Next question <ChevronRight size={16}/></button>:<button className="primary-button submit-button" disabled={examBusy} onClick={()=>submitExam(false)}>{examBusy?'Submitting…':'Submit exam'} <ArrowUpRight size={16}/></button>}</div></div> : examResult ? <div className="result-shell"><div className="result-hero"><div className="result-medal"><Trophy size={32}/></div><div className="eyebrow">EXAM COMPLETE</div><h2>{examResult.percentage>=80?'Excellent progress.':examResult.percentage>=60?'Good work. Keep building.':'Every attempt is progress.'}</h2><p>Your results are saved to your learning history.</p><div className="result-score"><b>{Math.round(examResult.percentage)}<small>%</small></b><span>{examResult.score} correct out of {examResult.totalQuestions}</span></div><div className="result-stats"><div><b>{examResult.score}</b><small>CORRECT</small></div><div><b>{examResult.totalQuestions-examResult.score}</b><small>TO REVIEW</small></div><div><b>{examResult.timedOut?'Yes':'No'}</b><small>TIME EXPIRED</small></div></div><div className="hero-cta"><button className="primary-button" onClick={()=>{setExamResult(null);setExam(null)}}><RefreshCw size={15}/> New mock exam</button><button className="ghost-button" onClick={()=>navigate('analytics')}>View analytics <ArrowRight size={15}/></button></div></div><div className="review-list"><div className="panel-heading"><div><div className="eyebrow">ANSWER REVIEW</div><h3>Learn from every question</h3></div></div>{examResult.review.map((r,i)=><div className={`review-card ${r.correct?'review-correct':'review-wrong'}`} key={r.questionId}><div className="review-heading"><span>Q{String(i+1).padStart(2,'0')}</span><b>{r.title}</b><strong>{r.correct?'CORRECT':'REVIEW'}</strong></div><div className="review-answers"><span>Your answer: <b>{r.selectedOption||'Not answered'}</b></span><span>Correct answer: <b>{r.correctOption}</b></span></div><p>{r.explanation}</p></div>)}</div></div> : <div className="practice-layout"><div className="practice-intro"><div className="practice-graphic"><div className="graphic-ring ring-1"/><div className="graphic-ring ring-2"/><div className="graphic-center"><BrainCircuit size={45}/><span>FOCUS</span></div><div className="graphic-orbit orbit-1"><Code2/></div><div className="graphic-orbit orbit-2"><Zap/></div></div><div className="eyebrow">THE INTERVIEW SIMULATOR</div><h2>Turn knowledge<br/>into <span>confidence.</span></h2><p>Timed sessions, instant scoring and explanations that help you understand why an answer is right.</p><div className="feature-line"><CheckCircle2/> Questions are shuffled for each attempt</div><div className="feature-line"><CheckCircle2/> Answers stay hidden until submission</div><div className="feature-line"><CheckCircle2/> Results are saved to your private history</div></div><form className="exam-config panel" onSubmit={startExam}><div className="eyebrow">CONFIGURE YOUR SESSION</div><h3>Build your mock exam</h3><p>Choose a focus or mix different technologies.</p><label>Technology focus<select value={examConfig.technology} onChange={e=>setExamConfig({...examConfig,technology:e.target.value})}><option value="">Mixed technologies</option>{['Java','Spring Boot','Spring Data JPA','Hibernate','REST API','MySQL','Redis','Kafka','Spring Security','Microservices'].map(t=><option key={t}>{t}</option>)}</select></label><label>Difficulty<select value={examConfig.difficulty} onChange={e=>setExamConfig({...examConfig,difficulty:e.target.value})}><option value="">All difficulty levels</option><option>Beginner</option><option>Intermediate</option><option>Advanced</option></select></label><div className="config-duo"><label>Questions<select value={examConfig.count} onChange={e=>setExamConfig({...examConfig,count:Number(e.target.value)})}>{[1,5,10,15,20].map(n=><option key={n} value={n}>{n} question{n===1?'':'s'}</option>)}</select></label><label>Duration<select value={examConfig.durationMinutes} onChange={e=>setExamConfig({...examConfig,durationMinutes:Number(e.target.value)})}>{[10,20,30,45].map(n=><option key={n} value={n}>{n} minutes</option>)}</select></label></div><div className="exam-config-note"><Clock3 size={16}/><span>Timer starts as soon as the exam loads. You can submit at any time.</span></div><button className="primary-button wide" disabled={examBusy}>{examBusy?'Preparing exam…':'Generate mock exam'} <ArrowUpRight size={16}/></button></form></div>}
      </section>}

      {view==='account' && <section className="page-section"><PageTitle eyebrow="YOUR ACCOUNT" title="Account settings" desc="Manage your sign-in credentials and keep your learning workspace secure."/><div className="account-settings-grid"><div className="panel account-profile-card"><div className="account-profile-avatar">{session?.user?.fullName?.slice(0,1).toUpperCase()||'U'}</div><div className="eyebrow">LEARNER PROFILE</div><h3>{session?.user?.fullName}</h3><p>{session?.user?.email}</p><span className="role-chip"><ShieldCheck size={13}/>{session?.user?.role}</span><div className="account-security-note"><CheckCircle2 size={16}/><span>Your session is limited to this browser tab and your password is stored as a one-way hash.</span></div></div><form className="panel password-card" onSubmit={changePassword}><div className="eyebrow">CREDENTIAL SECURITY</div><h3>Change password</h3><p>Choose a unique password with at least 8 characters.</p><label>Current password<input type="password" required autoComplete="current-password" value={passwordForm.currentPassword} onChange={e=>setPasswordForm({...passwordForm,currentPassword:e.target.value})}/></label><label>New password<input type="password" required minLength={8} maxLength={72} autoComplete="new-password" value={passwordForm.newPassword} onChange={e=>setPasswordForm({...passwordForm,newPassword:e.target.value})}/></label><button className="primary-button" disabled={passwordBusy}>{passwordBusy?'Updating…':'Update password'} <ArrowUpRight size={15}/></button></form></div></section>}

      {view==='bookmarks' && <section className="page-section"><PageTitle eyebrow="YOUR PERSONAL LIBRARY" title="Saved for when you need it." desc="Revisit concepts you want to strengthen before your next technical interview."/>{bookmarks.length?<div className="question-grid saved-grid">{bookmarks.map((item,index)=><QuestionCard key={item.bookmarkId} question={item.question} index={index} saved onSelect={()=>{setView('explore');openQuestion(item.question)}} onSave={()=>toggleBookmark(item.question.id)}/>)}</div>:<EmptyState icon={Bookmark} title="Your library is ready" text="Save questions while exploring and they will be collected here for quick revision." action="Explore questions" onAction={()=>navigate('explore')}/>}</section>}

      {view==='analytics' && <section className="page-section"><PageTitle eyebrow="PERSONAL PERFORMANCE" title="Progress you can see." desc="Use your results to understand where you are strong and where another round of practice can help."/>{analytics?<><div className="analytics-kpis"><Metric icon={Target} label="TOTAL ATTEMPTS" value={analytics.totalAttempts} detail="Completed mock exams"/><Metric icon={BookOpen} label="QUESTIONS IN EXAMS" value={analytics.totalExamQuestions} detail="Across completed attempts"/><Metric icon={Activity} label="AVERAGE SCORE" value={`${Math.round(analytics.averagePercentage)}%`} detail="Across completed exams"/><Metric icon={Award} label="PERSONAL BEST" value={`${Math.round(analytics.bestPercentage)}%`} detail="Highest exam score"/></div><div className="analytics-grid"><div className="panel"><div className="panel-heading"><div><div className="eyebrow">TOPIC BREAKDOWN</div><h3>Performance by technology</h3></div><BarChart3/></div>{analytics.technologyProgress.length?<div className="topic-bars">{analytics.technologyProgress.map(t=><div className="topic-bar" key={t.technology}><div><b>{t.technology}</b><span>{Math.round(t.averagePercentage)}% avg · {t.attempts} exams</span></div><i><span style={{width:`${Math.min(100,t.averagePercentage)}%`}}/></i></div>)}</div>:<EmptyState icon={BarChart3} title="No topic data yet" text="Complete a mock exam to see your topic performance."/>}</div><div className="panel history-panel"><div className="panel-heading"><div><div className="eyebrow">YOUR HISTORY</div><h3>Recent attempts</h3></div><Clock3/></div>{attempts.length?attempts.map(a=><AttemptRow key={a.id} attempt={a} onClick={()=>a.status==='COMPLETED'?openAttemptReview(a.id):a.status==='IN_PROGRESS'?resumeExam(a.id):undefined}/>):<div className="empty-inline">No completed attempts yet.</div>}</div></div></>:<EmptyState icon={BarChart3} title="Your analytics will appear here" text="Complete your first mock exam to start building a personal performance profile." action="Start a mock exam" onAction={openPractice}/>}</section>}

      {view==='admin' && isAdmin && <section className="page-section"><PageTitle eyebrow="PLATFORM MANAGEMENT" title="Admin studio" desc="Manage the knowledge bank and monitor how learners are using the platform."/><div className="admin-kpis">{adminOverview&&<><Metric icon={FileQuestion} label="ACTIVE QUESTIONS" value={adminOverview.activeQuestions} detail="Available in the library"/><Metric icon={Users} label="REGISTERED USERS" value={adminOverview.registeredUsers} detail="Platform accounts"/><Metric icon={Target} label="PRACTICE ATTEMPTS" value={adminOverview.practiceAttempts} detail="Sessions started"/><Metric icon={Bookmark} label="BOOKMARKS" value={adminOverview.bookmarkedQuestions} detail="Learner saves"/></>}</div><div className="admin-layout"><form className="panel question-editor" onSubmit={saveQuestion}><div className="panel-heading"><div><div className="eyebrow">CONTENT MANAGEMENT</div><h3>{editingId?'Edit question':'Publish a question'}</h3></div><span className="panel-icon"><Plus/></span></div><label>Question title<input required maxLength="500" value={questionForm.title} onChange={e=>setQuestionForm({...questionForm,title:e.target.value})} placeholder="e.g. How does HashMap work internally?"/></label><div className="config-duo"><label>Technology<input required value={questionForm.technology} onChange={e=>setQuestionForm({...questionForm,technology:e.target.value})}/></label><label>Category<input required value={questionForm.category} onChange={e=>setQuestionForm({...questionForm,category:e.target.value})}/></label></div><label>Difficulty<select value={questionForm.difficulty} onChange={e=>setQuestionForm({...questionForm,difficulty:e.target.value})}><option>Beginner</option><option>Intermediate</option><option>Advanced</option></select></label><label>Explanation<textarea required rows="4" value={questionForm.explanation} onChange={e=>setQuestionForm({...questionForm,explanation:e.target.value})} placeholder="Explain the concept clearly…"/></label><label>Code example <small>(optional)</small><textarea rows="3" value={questionForm.codeExample} onChange={e=>setQuestionForm({...questionForm,codeExample:e.target.value})} placeholder="// Add a Java example"/></label><div className="editor-subhead"><b>Multiple-choice options</b><span>All four are required</span></div>{['A','B','C','D'].map(k=><label className="option-editor" key={k}><b>{k}</b><input required value={questionForm[`option${k}`]} onChange={e=>setQuestionForm({...questionForm,[`option${k}`]:e.target.value})} placeholder={`Option ${k}`}/></label>)}<label>Correct answer<select value={questionForm.correctOption} onChange={e=>setQuestionForm({...questionForm,correctOption:e.target.value})}>{['A','B','C','D'].map(k=><option key={k} value={k}>Option {k}</option>)}</select></label><div className="editor-actions"><button className="primary-button" disabled={savingQuestion}>{savingQuestion?'Saving…':editingId?'Update question':'Publish question'} <ArrowUpRight size={15}/></button>{editingId&&<button className="ghost-button" type="button" onClick={()=>{setEditingId(null);setQuestionForm(emptyQuestion)}}>Cancel</button>}</div></form><div className="panel question-manager"><div className="panel-heading"><div><div className="eyebrow">QUESTION LIBRARY</div><h3>Manage questions</h3></div><span className="tag">{questions.length} loaded</span></div>{questions.map(q=><div className="manage-row" key={q.id}><div><b>{q.title}</b><small>{q.technology} · {q.difficulty}</small></div><div><button className="small-action" onClick={()=>editQuestion(q)}>Edit</button><button className="small-action danger" onClick={()=>archiveQuestion(q.id)}>Archive</button></div></div>)}<details className="bulk-import"><summary><Plus size={14}/> Bulk import JSON</summary><form onSubmit={importBulkQuestions}><p>Import up to 100 questions in one request. Each item uses the same fields as the editor.</p><textarea rows="9" value={bulkJson} onChange={e=>setBulkJson(e.target.value)} placeholder={'[{ "title":"...", "technology":"Java", "category":"Collections", "difficulty":"Beginner", "explanation":"...", "optionA":"...", "optionB":"...", "optionC":"...", "optionD":"...", "correctOption":"A" }]'}/><button className="primary-button" disabled={bulkBusy||!bulkJson.trim()}>{bulkBusy?'Importing…':'Import questions'} <ArrowUpRight size={15}/></button></form></details></div></div></section>}

      <footer className="app-footer"><span>BACKENDHUB <i>•</i> JAVA BACKEND INTERVIEW PREP</span><span>DESIGNED FOR THE NEXT GENERATION OF BUILDERS <Sparkles size={12}/></span></footer>
    </main>
    {toast&&<div className="toast"><CheckCircle2 size={17}/>{toast}<button onClick={()=>setToast('')}><X size={15}/></button></div>}
  </div>;
}

function Metric({icon:Icon,label,value,detail}) { return <div className="metric-card"><div className="metric-icon"><Icon size={18}/></div><div className="metric-copy"><span>{label}</span><b>{value}</b><small>{detail}</small></div><ArrowUpRight className="metric-arrow" size={15}/></div>; }
function PageTitle({eyebrow,title,desc}) { return <div className="page-title"><div className="eyebrow"><span className="eyebrow-line"/>{eyebrow}</div><h1>{title}</h1><p>{desc}</p></div>; }
function Difficulty({value}) { return <span className={`difficulty ${String(value||'').toLowerCase()}`}>{value}</span>; }
function QuestionCard({question,index,saved,selected,onSelect,onSave,onEdit}) { return <article className={`question-card ${selected?'is-selected':''}`} onClick={onSelect}><div className="card-topline"><span className="tag">{question.technology}</span><Difficulty value={question.difficulty}/></div><h3>{question.title}</h3><p>{question.category}</p><div className="card-foot"><span>CONCEPT {String(index+1).padStart(2,'0')}</span><div><button className={`card-icon ${saved?'saved':''}`} title={saved?'Remove bookmark':'Save question'} onClick={e=>{e.stopPropagation();onSave?.()}}>{saved?<BookmarkCheck size={16}/>:<Bookmark size={16}/>}</button>{onEdit&&<button className="card-icon" title="Edit question" onClick={e=>{e.stopPropagation();onEdit()}}><FileQuestion size={16}/></button>}<ChevronRight size={16}/></div></div></article>; }
function ActionRow({icon:Icon,title,desc,action,onClick}) { return <button className="action-row" onClick={onClick}><span className="action-icon"><Icon size={18}/></span><span className="action-copy"><b>{title}</b><small>{desc}</small></span><span className="action-cta">{action}<ArrowUpRight size={14}/></span></button>; }
function AttemptRow({attempt,onClick}) { return <button className={`attempt-row ${onClick?'clickable':''}`} onClick={onClick} disabled={!onClick}><div className="attempt-icon"><Target size={16}/></div><div className="attempt-main"><b>{attempt.technology} mock exam</b><small>{attempt.difficulty} · {attempt.totalQuestions} questions · {new Date(attempt.startedAt).toLocaleDateString()}</small></div><div className="attempt-score"><b>{attempt.status==='COMPLETED'?`${Math.round(attempt.percentage)}%`:'In progress'}</b><small>{attempt.status==='IN_PROGRESS'?'Resume saved session':`${attempt.score}/${attempt.totalQuestions} correct`}</small></div></button>; }
function EmptyState({icon:Icon,title,text,action,onAction}) { return <div className="empty-state"><div><Icon size={22}/></div><h3>{title}</h3><p>{text}</p>{action&&<button className="primary-button" onClick={onAction}>{action}<ArrowRight size={15}/></button>}</div>; }
function DatabaseIcon() { return <Layers size={15}/>; }

createRoot(document.getElementById('root')).render(<App/>);
