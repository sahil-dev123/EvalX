/* ==========================================================================
   EvalX — SPA Application
   ========================================================================== */

const API = '';

// ── State ──────────────────────────────────────────────────────────────────
const state = {
  page: 'home',
  userRole: localStorage.getItem('evalx_user_role') || null,
  exams: [],
  selectedExam: null,
  stages: [],
  selectedStage: null,
  years: [],
  selectedYear: null,
  examYearDetails: null,
  uploadedFile: null,
  result: null,
  loading: false,
  adminSection: 'magic-ingest',
  adminAdvancedOpen: false,
  adminToken: localStorage.getItem('evalx_admin_token') || null,
  adminExams: [],
  adminStages: [],
  adminYears: [],
  adminSections: [],
  adminQuestions: [],
  adminPolicies: [],
  // Evaluator state
  evaluatorResponses: [],
  currentEvaluation: null,
  // Student state
  studentExams: [],
  studentResults: [],
};

// ── API Client ─────────────────────────────────────────────────────────────
async function api(method, path, body, isFormData = false) {
  const opts = { method, headers: {} };
  if (body && !isFormData) {
    opts.headers['Content-Type'] = 'application/json';
    opts.body = JSON.stringify(body);
  } else if (body && isFormData) {
    opts.body = body;
  }
  if (state.adminToken) {
    opts.headers['Authorization'] = `Bearer ${state.adminToken}`;
  }
  try {
    const res = await fetch(`${API}${path}`, opts);
    const json = await res.json();
    if (res.status === 401 && state.adminToken && path !== '/api/auth/login') {
      state.adminToken = null;
      localStorage.removeItem('evalx_admin_token');
      toast('Session expired, please login again', 'error');
      if (state.page === 'admin') navigate('admin');
    }
    if (!res.ok || !json.success) throw new Error(json.message || 'API Error');
    return json.data;
  } catch (error) {
    console.error('API Error:', error);
    throw error;
  }
}

// ── Role Management ───────────────────────────────────────────────────────
function setUserRole(role) {
  state.userRole = role;
  localStorage.setItem('evalx_user_role', role);
  updateRoleIndicator();
  updateNavigation();
}

function updateRoleIndicator() {
  const roleIndicator = document.getElementById('role-indicator');
  if (!roleIndicator) return;

  const roleName = state.userRole ? state.userRole.charAt(0).toUpperCase() + state.userRole.slice(1) : null;
  const roleIcons = { admin: '👨‍💼', evaluator: '✅', student: '📚' };

  if (state.userRole && state.adminToken) {
    roleIndicator.innerHTML = `${roleIcons[state.userRole] || ''} ${roleName}`;
    roleIndicator.className = `role-badge role-${state.userRole}`;
    roleIndicator.classList.remove('hidden');
  } else {
    roleIndicator.classList.add('hidden');
  }
}

function updateNavigation() {
  const navHome = document.getElementById('nav-home');
  const navAdmin = document.getElementById('nav-admin');
  const navEvaluator = document.getElementById('nav-evaluator');
  const navStudent = document.getElementById('nav-student');

  if (navHome) navHome.classList.add('active');
  if (navAdmin) navAdmin.classList.toggle('hidden', state.userRole !== 'admin' || !state.adminToken);
  if (navEvaluator) navEvaluator.classList.toggle('hidden', state.userRole !== 'evaluator' || !state.adminToken);
  if (navStudent) navStudent.classList.toggle('hidden', state.userRole !== 'student' || !state.adminToken);
}

// ── Router ─────────────────────────────────────────────────────────────────
function navigate(page, data) {
  state.page = page;
  if (data) Object.assign(state, data);
  render();
  window.scrollTo({ top: 0, behavior: 'smooth' });

  // Update URL without refresh
  const path = page === 'home' ? '/' : `/${page}`;
  if (window.location.pathname !== path) {
    window.history.pushState({ page }, '', path);
  }

  // Update nav
  document.querySelectorAll('.nav-links button').forEach(b => b.classList.remove('active'));
  const navEl = document.getElementById(`nav-${page === 'admin' ? 'admin' : page === 'evaluator' ? 'evaluator' : page === 'student' ? 'student' : 'home'}`);
  if (navEl) navEl.classList.add('active');
}

// ── Toast ──────────────────────────────────────────────────────────────────
function toast(msg, type = 'info') {
  const el = document.createElement('div');
  el.className = `toast toast-${type}`;
  el.innerHTML = `<span>${type === 'success' ? '✓' : type === 'error' ? '✗' : 'ℹ'}</span><span>${msg}</span>`;
  document.getElementById('toast-container').appendChild(el);
  setTimeout(() => { el.style.animation = 'slideOut 0.3s forwards'; setTimeout(() => el.remove(), 300); }, 3500);
}

// ── Render ─────────────────────────────────────────────────────────────────
function render() {
  const app = document.getElementById('app');
  switch (state.page) {
    case 'home': app.innerHTML = renderHome(); loadExams(); break;
    case 'stages': app.innerHTML = renderStages(); break;
    case 'years': app.innerHTML = renderYears(); break;
    case 'upload': app.innerHTML = renderUpload(); break;
    case 'result': app.innerHTML = renderResult(); initCharts(); break;
    case 'admin': app.innerHTML = renderAdmin(); break;
    case 'evaluator': app.innerHTML = renderEvaluator(); break;
    case 'student': app.innerHTML = renderStudent(); break;
    default: app.innerHTML = renderHome(); loadExams();
  }
}

// ── Home Page ──────────────────────────────────────────────────────────────
function renderHome() {
  return `
    <div class="hero fade-in">
      <div class="container">
        <h1>Predict Your <span class="text-gradient">Exam Rank</span> Instantly</h1>
        <p>Upload your response sheet and get instant marks calculation, predicted percentile, and estimated rank based on official answer keys and statistical modeling.</p>
        <div class="flex justify-center gap-2">
          <span class="badge badge-success">✓ No Login Required</span>
          <span class="badge badge-info">⚡ Instant Results</span>
        </div>
      </div>
    </div>
    <div class="section">
      <div class="container">
        <h2 class="mb-3">Select Exam</h2>
        <div class="grid grid-3 exam-grid" id="exam-grid">
          ${state.loading ? '<div class="loading-overlay"><div class="spinner"></div><p>Loading exams...</p></div>' :
      state.exams.length === 0 ? '<div class="empty-state"><div class="icon">📋</div><p>No exams configured yet. Go to Admin to create one.</p></div>' :
        state.exams.map(e => `
              <div class="card exam-card fade-in" onclick="selectExam(${e.id})">
                <div class="exam-icon">📝</div>
                <h3>${e.name}</h3>
                <p>${e.description || 'Click to view available stages'}</p>
                <span class="tag">${e.stages?.length || 0} Stage${(e.stages?.length || 0) !== 1 ? 's' : ''}</span>
              </div>
            `).join('')}
        </div>
      </div>
    </div>
    <div class="section" id="universal-upload-section">
      <div class="container">
        <div class="card premium-card fade-in">
          <div class="flex items-center gap-2 mb-2">
            <span class="badge badge-success">Magic Auto-Detect</span>
            <h2>Universal Response Upload</h2>
          </div>
          <p class="text-secondary mb-3">Skip the manual selection! Drag and drop your GATE, SSC, or CAT response sheet PDF here. Our AI-ready engine will automatically detect your exam, shift, and year.</p>
          
          <div class="upload-zone" id="universal-upload-zone" 
               ondragover="event.preventDefault(); this.classList.add('dragover')"
               ondragleave="this.classList.remove('dragover')"
               ondrop="handleUniversalDrop(event)"
               onclick="document.getElementById('universal-file-input').click()">
            <input type="file" id="universal-file-input" accept=".pdf,.csv,.json" onchange="handleUniversalFile(event)">
            <div class="icon">✨</div>
            <h3>Drop your response sheet here</h3>
            <p class="text-muted">Supports PDF, CSV, and JSON</p>
            <div id="universal-file-info" class="file-info hidden"></div>
          </div>
          <button class="btn btn-primary btn-lg mt-3 w-100" id="universal-upload-btn" onclick="uploadUniversalResponse()" disabled>⚡ Calculate My Score</button>
        </div>
      </div>
    </div>
    <div class="section" style="border-top: 1px solid var(--border);">
      <div class="container">
        <div class="grid grid-3">
          <div class="card stat-card"><div class="stat-value text-gradient">100%</div><div class="stat-label">Data Driven</div></div>
          <div class="card stat-card"><div class="stat-value text-gradient">AI Ready</div><div class="stat-label">Architecture</div></div>
          <div class="card stat-card"><div class="stat-value text-gradient">2.5M+</div><div class="stat-label">Scale Ready</div></div>
        </div>
      </div>
    </div>`;
}

let universalFile = null;

function handleUniversalDrop(e) {
  e.preventDefault(); e.currentTarget.classList.remove('dragover');
  universalFile = e.dataTransfer.files[0]; showUniversalFileInfo();
}
function handleUniversalFile(e) { universalFile = e.target.files[0]; showUniversalFileInfo(); }
function showUniversalFileInfo() {
  if (!universalFile) return;
  const info = document.getElementById('universal-file-info');
  info.textContent = `✓ ${universalFile.name}`;
  info.classList.remove('hidden');
  document.getElementById('universal-upload-btn').disabled = false;
}

async function uploadUniversalResponse() {
  if (!universalFile) return;
  state.loading = true;
  render();
  const originalBtn = document.getElementById('universal-upload-btn');
  if (originalBtn) {
    originalBtn.disabled = true;
    originalBtn.textContent = '⏳ Processing your response...';
  }
  try {
    const formData = new FormData();
    formData.append('file', universalFile);
    const result = await api('POST', '/api/evaluation/evaluate', formData, true);
    state.result = result;
    navigate('result');
    toast('✓ Evaluation Complete!', 'success');
  } catch (e) {
    toast('✗ Evaluation Failed: ' + e.message, 'error');
    state.loading = false;
    if (originalBtn) {
      originalBtn.disabled = false;
      originalBtn.textContent = '⚡ Calculate My Score';
    }
    render();
  }
}

async function loadExams() {
  if (state.exams.length > 0) return;
  state.loading = true;
  document.getElementById('exam-grid').innerHTML = '<div class="loading-overlay"><div class="spinner"></div><p>Loading exams...</p></div>';
  try {
    state.exams = await api('GET', '/api/exams');
    state.loading = false;
    document.getElementById('exam-grid').innerHTML = state.exams.length === 0 ?
      '<div class="empty-state"><div class="icon">📋</div><p>No exams yet. Use Admin to create one.</p></div>' :
      state.exams.map(e => `
        <div class="card exam-card fade-in" onclick="selectExam(${e.id})">
          <div class="exam-icon">📝</div>
          <h3>${e.name}</h3>
          <p>${e.description || 'Click to view available stages'}</p>
          <span class="tag">${e.stages?.length || 0} Stage${(e.stages?.length || 0) !== 1 ? 's' : ''}</span>
        </div>
      `).join('');
  } catch (e) { toast('Failed to load exams: ' + e.message, 'error'); state.loading = false; }
}

async function selectExam(id) {
  try {
    const exam = state.exams.find(e => e.id === id);
    state.selectedExam = exam;
    state.stages = await api('GET', `/api/exam-stages/by-exam/${id}`);
    navigate('stages');
  } catch (e) { toast('Error: ' + e.message, 'error'); }
}

// ── Stage Selection ────────────────────────────────────────────────────────
function renderStages() {
  const stageColors = ['var(--gradient-1)', 'var(--gradient-2)', 'var(--gradient-3)'];
  return `
    <div class="page">
      <div class="container fade-in">
        <div class="breadcrumb">
          <a href="#" onclick="navigate('home')">Home</a>
          <span class="sep">›</span>
          <span class="current">${state.selectedExam?.name}</span>
        </div>
        <h2 class="mb-3">Select Stage</h2>
        <div class="grid grid-2">
          ${state.stages.map((s, i) => `
            <div class="card selection-card fade-in" onclick="selectStage(${s.id})" style="animation-delay: ${i * 0.1}s">
              <div class="icon-box" style="background: ${stageColors[i % 3]}; color: white;">
                ${i + 1}
              </div>
              <div>
                <h3>${s.name}</h3>
                <p>${s.description || ''}</p>
                <p class="text-muted" style="margin-top:4px;">${s.years?.length || 0} year(s) available</p>
              </div>
              <div class="arrow">→</div>
            </div>
          `).join('')}
        </div>
      </div>
    </div>`;
}

async function selectStage(id) {
  try {
    const stage = state.stages.find(s => s.id === id);
    state.selectedStage = stage;
    state.years = await api('GET', `/api/exam-years/by-stage/${id}`);
    navigate('years');
  } catch (e) { toast('Error: ' + e.message, 'error'); }
}

// ── Year Selection ─────────────────────────────────────────────────────────
function renderYears() {
  return `
    <div class="page">
      <div class="container fade-in">
        <div class="breadcrumb">
          <a href="#" onclick="navigate('home')">Home</a>
          <span class="sep">›</span>
          <a href="#" onclick="selectExam(${state.selectedExam?.id})">${state.selectedExam?.name}</a>
          <span class="sep">›</span>
          <span class="current">${state.selectedStage?.name}</span>
        </div>
        <h2 class="mb-3">Select Year</h2>
        <div class="grid grid-3">
          ${state.years.map((y, i) => `
            <div class="card selection-card fade-in" onclick="selectYear(${y.id})" style="animation-delay: ${i * 0.1}s">
              <div class="icon-box" style="background: var(--gradient-2); color: white;">
                📅
              </div>
              <div>
                <h3>${y.year}</h3>
                <p>${y.totalMarks ? y.totalMarks + ' marks' : ''} ${y.timeMinutes ? '• ' + y.timeMinutes + ' min' : ''}</p>
                <p class="text-muted">${y.sections?.length || 0} sections • ${(y.totalCandidates || 0).toLocaleString()} candidates</p>
              </div>
              <div class="arrow">→</div>
            </div>
          `).join('')}
          ${state.years.length === 0 ? '<div class="empty-state"><div class="icon">📅</div><p>No years configured for this stage yet.</p></div>' : ''}
        </div>
      </div>
    </div>`;
}

async function selectYear(id) {
  try {
    state.selectedYear = state.years.find(y => y.id === id);
    state.examYearDetails = state.selectedYear;
    navigate('upload');
  } catch (e) { toast('Error: ' + e.message, 'error'); }
}

// ── Upload Page ────────────────────────────────────────────────────────────
function renderUpload() {
  const y = state.selectedYear;
  return `
    <div class="page">
      <div class="container fade-in">
        <div class="breadcrumb">
          <a href="#" onclick="navigate('home')">Home</a>
          <span class="sep">›</span>
          <a href="#" onclick="selectExam(${state.selectedExam?.id})">${state.selectedExam?.name}</a>
          <span class="sep">›</span>
          <a href="#" onclick="selectStage(${state.selectedStage?.id})">${state.selectedStage?.name}</a>
          <span class="sep">›</span>
          <span class="current">${y?.year}</span>
        </div>

        <div style="max-width: 700px; margin: 0 auto;">
          <h2 class="mb-2" style="text-align:center;">Upload Response Sheet</h2>
          <p class="text-secondary mb-4" style="text-align:center;">
            ${state.selectedExam?.name} — ${state.selectedStage?.name} — ${y?.year}
          </p>

          <!-- Exam Info -->
          <div class="card mb-3">
            <div class="grid grid-3" style="text-align:center;">
              <div><div style="font-size:1.5rem;font-weight:800;">${y?.totalMarks || '—'}</div><div class="text-muted" style="font-size:0.8rem;">Total Marks</div></div>
              <div><div style="font-size:1.5rem;font-weight:800;">${y?.timeMinutes || '—'}</div><div class="text-muted" style="font-size:0.8rem;">Minutes</div></div>
              <div><div style="font-size:1.5rem;font-weight:800;">${y?.sections?.length || '—'}</div><div class="text-muted" style="font-size:0.8rem;">Sections</div></div>
            </div>
          </div>

          <!-- Upload Zone -->
          <div class="upload-zone" id="upload-zone"
               ondragover="event.preventDefault(); this.classList.add('dragover')"
               ondragleave="this.classList.remove('dragover')"
               ondrop="handleDrop(event)"
               onclick="document.getElementById('file-input').click()">
            <input type="file" id="file-input" accept=".csv,.json,.pdf" onchange="handleFileSelect(event)">
            <div class="icon">📄</div>
            <h3>Drop your response sheet here</h3>
            <p>or click to browse • Supports PDF, CSV, JSON</p>
            <p class="text-muted mt-2" style="font-size:0.8rem;">Format: questionId, answer (e.g., 1,B)</p>
            <div id="file-info" class="file-info hidden"></div>
          </div>

          <!-- Submit -->
          <button class="btn btn-primary btn-lg w-full mt-3" id="submit-btn" onclick="submitResponse()" disabled>
            ⚡ Evaluate My Answers
          </button>

          <div id="upload-loading" class="loading-overlay hidden">
            <div class="spinner"></div>
            <p>Evaluating your responses...</p>
          </div>
        </div>
      </div>
    </div>`;
}

function handleDrop(e) {
  e.preventDefault();
  e.currentTarget.classList.remove('dragover');
  const file = e.dataTransfer.files[0];
  if (file) processFile(file);
}

function handleFileSelect(e) {
  const file = e.target.files[0];
  if (file) processFile(file);
}

function processFile(file) {
  const ext = file.name.split('.').pop().toLowerCase();
  if (!['csv', 'json', 'pdf'].includes(ext)) {
    toast('Unsupported format. Please upload PDF, CSV or JSON.', 'error');
    return;
  }
  state.uploadedFile = file;
  document.getElementById('file-info').textContent = `✓ ${file.name} (${(file.size / 1024).toFixed(1)} KB)`;
  document.getElementById('file-info').classList.remove('hidden');
  document.getElementById('submit-btn').disabled = false;
  toast('File selected: ' + file.name, 'success');
}

async function submitResponse() {
  if (!state.uploadedFile) return;
  document.getElementById('submit-btn').disabled = true;
  document.getElementById('upload-loading').classList.remove('hidden');

  try {
    const formData = new FormData();
    formData.append('file', state.uploadedFile);
    const res = await fetch(`${API}/api/evaluation/evaluate?examYearId=${state.selectedYear.id}`, {
      method: 'POST', body: formData
    });
    const json = await res.json();
    if (!json.success) throw new Error(json.message);
    state.result = json.data;
    toast('Evaluation complete!', 'success');
    navigate('result');
  } catch (e) {
    toast('Evaluation failed: ' + e.message, 'error');
    document.getElementById('submit-btn').disabled = false;
    document.getElementById('upload-loading').classList.add('hidden');
  }
}

// ── Result Page ────────────────────────────────────────────────────────────
function renderResult() {
  const r = state.result;
  if (!r) return '<div class="page"><div class="container"><p>No result data.</p></div></div>';

  const accuracy = r.totalQuestions > 0 ? ((r.correct / r.totalQuestions) * 100).toFixed(1) : 0;

  return `
    <div class="page fade-in">
      <div class="result-hero">
        <div class="container">
          <p class="text-secondary">${r.examName || state.selectedExam?.name || 'Exam'} — ${r.stageName || state.selectedStage?.name || 'Stage'} — ${r.year || state.selectedYear?.year || 'Year'}</p>
          <div class="score-display text-gradient">${r.totalScore} / ${r.maxScore}</div>
          <p class="score-sub">${r.shiftName ? r.shiftName : 'Your Score'}</p>
        </div>
      </div>

      <div class="section">
        <div class="container">
          <!-- Key Stats -->
          <div class="grid grid-4 mb-4">
            <div class="card stat-card success"><div class="stat-value">${r.correct}</div><div class="stat-label">Correct</div></div>
            <div class="card stat-card danger"><div class="stat-value">${r.incorrect}</div><div class="stat-label">Incorrect</div></div>
            <div class="card stat-card warning"><div class="stat-value">${r.skipped}</div><div class="stat-label">Skipped</div></div>
            <div class="card stat-card info"><div class="stat-value">${accuracy}%</div><div class="stat-label">Accuracy</div></div>
          </div>

          <!-- Rank Prediction -->
          <div class="grid grid-3 mb-4">
            <div class="card stat-card accent"><div class="stat-value">${r.percentile || '—'}%</div><div class="stat-label">Predicted Percentile</div></div>
            <div class="card stat-card accent"><div class="stat-value">${r.estimatedRank ? r.estimatedRank.toLocaleString() : '—'}</div><div class="stat-label">Estimated Rank</div></div>
            <div class="card stat-card accent"><div class="stat-value">${r.totalCandidates ? r.totalCandidates.toLocaleString() : '—'}</div><div class="stat-label">Total Candidates</div></div>
          </div>

          <!-- Charts row -->
          <div class="grid grid-2 mb-4">
            <div class="card">
              <h3 class="mb-2">Score Distribution</h3>
              <div class="chart-container"><canvas id="distChart"></canvas></div>
            </div>
            <div class="card">
              <h3 class="mb-2">Section-wise Performance</h3>
              <div class="chart-container"><canvas id="sectionChart"></canvas></div>
            </div>
          </div>

          <!-- Analytics -->
          ${r.analytics ? `
          <div class="grid grid-2 mb-4">
            <div class="card">
              <h3 class="mb-2">Analytics Summary</h3>
              <table class="results-table">
                <tr><td class="text-secondary">Average Score</td><td style="font-weight:600;">${r.analytics.averageScore}</td></tr>
                <tr><td class="text-secondary">Highest Score</td><td style="font-weight:600;">${r.analytics.highestScore}</td></tr>
                <tr><td class="text-secondary">Lowest Score</td><td style="font-weight:600;">${r.analytics.lowestScore}</td></tr>
                <tr><td class="text-secondary">Standard Deviation</td><td style="font-weight:600;">${r.analytics.standardDeviation}</td></tr>
                <tr><td class="text-secondary">Expected Cutoff</td><td style="font-weight:600;color:var(--accent-light);">${r.analytics.expectedCutoff}</td></tr>
              </table>
            </div>
            <div class="card">
              <h3 class="mb-2">Difficulty Analysis</h3>
              <div class="chart-container"><canvas id="difficultyChart"></canvas></div>
            </div>
          </div>` : ''}

          <!-- Section-wise results table -->
          <div class="card mb-4">
            <h3 class="mb-2">Section-wise Breakdown</h3>
            <table class="results-table">
              <thead><tr><th>Section</th><th>Score</th><th>Correct</th><th>Incorrect</th><th>Skipped</th><th>Accuracy</th></tr></thead>
              <tbody>
                ${(r.sectionResults || []).map(s => {
    const pct = s.maxScore > 0 ? (s.score / s.maxScore * 100) : 0;
    const cls = pct >= 70 ? 'good' : pct >= 40 ? 'avg' : 'low';
    return `<tr>
                    <td style="font-weight:600;">${s.sectionName}</td>
                    <td>${s.score} / ${s.maxScore}</td>
                    <td style="color:var(--success)">${s.correct}</td>
                    <td style="color:var(--danger)">${s.incorrect}</td>
                    <td style="color:var(--warning)">${s.skipped}</td>
                    <td>
                      <div class="flex items-center gap-1">
                        <span>${s.accuracy}%</span>
                        <div class="progress-bar" style="flex:1;"><div class="progress-fill ${cls}" style="width:${s.accuracy}%"></div></div>
                      </div>
                    </td>
                  </tr>`;
  }).join('')}
              </tbody>
            </table>
          </div>

          <div style="text-align:center;">
            <button class="btn btn-primary btn-lg" onclick="navigate('upload')">↻ Try Again</button>
            <button class="btn btn-secondary btn-lg" onclick="navigate('home')" style="margin-left:12px;">← Back to Home</button>
          </div>
        </div>
      </div>
    </div>`;
}

function initCharts() {
  const r = state.result;
  if (!r) return;
  setTimeout(() => {
    // Score Distribution Chart
    const distCtx = document.getElementById('distChart')?.getContext('2d');
    if (distCtx && r.scoreDistribution?.length > 0) {
      new Chart(distCtx, {
        type: 'bar',
        data: {
          labels: r.scoreDistribution.map(d => d.bucket),
          datasets: [{
            label: 'Students',
            data: r.scoreDistribution.map(d => d.frequency),
            backgroundColor: r.scoreDistribution.map(d => d.isCandidateBucket ? 'rgba(99,102,241,0.8)' : 'rgba(99,102,241,0.2)'),
            borderColor: r.scoreDistribution.map(d => d.isCandidateBucket ? '#6366f1' : 'rgba(99,102,241,0.4)'),
            borderWidth: 1, borderRadius: 6,
          }]
        },
        options: { responsive: true, plugins: { legend: { display: false } }, scales: { x: { grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: '#8b8b9e', font: { size: 10 } } }, y: { grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: '#8b8b9e' } } } }
      });
    }

    // Section Performance Chart
    const secCtx = document.getElementById('sectionChart')?.getContext('2d');
    if (secCtx && r.sectionResults?.length > 0) {
      new Chart(secCtx, {
        type: 'radar',
        data: {
          labels: r.sectionResults.map(s => s.sectionName.length > 15 ? s.sectionName.substring(0, 15) + '...' : s.sectionName),
          datasets: [{
            label: 'Accuracy %',
            data: r.sectionResults.map(s => s.accuracy),
            fill: true,
            backgroundColor: 'rgba(99,102,241,0.15)',
            borderColor: '#6366f1',
            pointBackgroundColor: '#6366f1',
            pointBorderColor: '#fff',
          }]
        },
        options: { responsive: true, plugins: { legend: { display: false } }, scales: { r: { beginAtZero: true, max: 100, grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: '#8b8b9e', backdropColor: 'transparent' }, pointLabels: { color: '#8b8b9e', font: { size: 10 } } } } }
      });
    }

    // Difficulty Chart
    const diffCtx = document.getElementById('difficultyChart')?.getContext('2d');
    if (diffCtx && r.analytics?.difficultyAnalysis) {
      const labels = Object.keys(r.analytics.difficultyAnalysis);
      const values = Object.values(r.analytics.difficultyAnalysis);
      new Chart(diffCtx, {
        type: 'doughnut',
        data: {
          labels: labels.map(l => l.length > 20 ? l.substring(0, 20) + '...' : l),
          datasets: [{
            data: values,
            backgroundColor: ['#6366f1', '#06b6d4', '#f59e0b', '#22c55e', '#ef4444'],
            borderColor: 'rgba(0,0,0,0.3)', borderWidth: 2,
          }]
        },
        options: { responsive: true, plugins: { legend: { position: 'bottom', labels: { color: '#8b8b9e', font: { size: 11 }, padding: 12 } } } }
      });
    }
  }, 100);
}

// ── Evaluator Panel ────────────────────────────────────────────────────────

function renderEvaluator() {
  if (!state.adminToken) return renderAdminLogin();
  return `
    <div class="main-layout">
      <aside class="main-sidebar">
        <div class="sidebar-section">
          <div class="sidebar-label">📋 Queue</div>
          <button class="sidebar-item active" onclick="switchEvaluatorView('batch')">
            <span>📑</span> Batch Evaluation
          </button>
          <button class="sidebar-item" onclick="switchEvaluatorView('assigned')">
            <span>✅</span> My Assignments
          </button>
        </div>
        <div class="sidebar-section">
          <div class="sidebar-label">📊 Stats</div>
          <button class="sidebar-item" onclick="switchEvaluatorView('progress')">
            <span>📈</span> My Progress
          </button>
        </div>
        <div style="margin-top: auto; padding-top: 24px;">
          <button class="sidebar-item" style="color: var(--danger)" onclick="evaluatorLogout()">
            <span>🚪</span> Logout
          </button>
        </div>
      </aside>
      <main class="main-content fade-in">
        <div class="flex justify-between items-center mb-3">
          <h2>Batch Evaluation</h2>
          <div class="flex gap-1">
            <button class="btn btn-secondary btn-sm" onclick="loadBatchResponses()">🔄 Refresh</button>
          </div>
        </div>
        <div id="evaluator-main">
          ${renderBatchEvaluation()}
        </div>
      </main>
    </div>`;
}

function renderBatchEvaluation() {
  loadBatchResponses();
  return `
    <div class="card premium-card mb-3">
      <div class="flex justify-between items-center" style="flex-wrap: wrap; gap: 16px;">
        <div>
          <h3 class="mb-1">Pending Evaluations</h3>
          <p class="text-secondary" style="font-size:0.85rem;">Click on a response to start evaluating</p>
        </div>
        <div class="eval-filter">
          <div class="eval-filter-group">
            <label>Exam</label>
            <select class="form-input" id="eval-filter-exam" onchange="loadBatchResponses()" style="width:180px;">
              <option value="">All Exams</option>
              <option value="gate">GATE</option>
              <option value="ssc">SSC CGL</option>
              <option value="cat">CAT</option>
            </select>
          </div>
          <div class="eval-filter-group">
            <label>Status</label>
            <select class="form-input" id="eval-filter-status" onchange="loadBatchResponses()" style="width:140px;">
              <option value="">All Status</option>
              <option value="submitted">Submitted</option>
              <option value="in-progress">In Progress</option>
              <option value="evaluated">Evaluated</option>
            </select>
          </div>
          <button class="btn btn-secondary btn-sm" onclick="loadBatchResponses()" style="align-self: flex-end;">🔄 Refresh</button>
        </div>
      </div>
    </div>
    <div class="card">
      <table class="evaluation-table">
        <thead>
          <tr>
            <th style="width: 25%;">Student Name</th>
            <th style="width: 20%;">Exam</th>
            <th style="width: 20%;">Submitted</th>
            <th style="width: 15%;">Status</th>
            <th style="width: 20%;">Action</th>
          </tr>
        </thead>
        <tbody id="eval-batch-tbody">
          <tr><td colspan="5" style="text-align:center; padding:40px;"><div class="loading-overlay"><div class="spinner"></div></div></td></tr>
        </tbody>
      </table>
    </div>`;
}

function switchEvaluatorView(view) {
  document.querySelectorAll('.main-sidebar .sidebar-item').forEach(b => b.classList.remove('active'));
  const btn = event?.target?.closest('.sidebar-item');
  if (btn) btn.classList.add('active');

  const main = document.getElementById('evaluator-main');
  if (view === 'batch' && main) {
    main.innerHTML = renderBatchEvaluation();
  } else if (view === 'assigned' && main) {
    main.innerHTML = renderAssignedResponses();
  } else if (view === 'progress' && main) {
    main.innerHTML = renderEvaluatorProgress();
  }
}

function renderAssignedResponses() {
  return `
    <h2 class="mb-3">My Assigned Responses</h2>
    <div class="card">
      <table class="evaluation-table">
        <thead>
          <tr>
            <th>Student Name</th>
            <th>Exam</th>
            <th>Assigned On</th>
            <th>Progress</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td colspan="5" style="text-align:center; padding:40px;">
              <div class="empty-state">
                <div class="icon">📋</div>
                <p>No responses assigned to you yet</p>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>`;
}

function renderEvaluatorProgress() {
  return `
    <h2 class="mb-3">Your Progress</h2>
    <div class="grid grid-4 mb-4">
      <div class="card stat-card accent">
        <div class="stat-value">0</div>
        <div class="stat-label">Completed</div>
      </div>
      <div class="card stat-card info">
        <div class="stat-value">0</div>
        <div class="stat-label">In Progress</div>
      </div>
      <div class="card stat-card warning">
        <div class="stat-value">0</div>
        <div class="stat-label">Pending</div>
      </div>
      <div class="card stat-card success">
        <div class="stat-value">0%</div>
        <div class="stat-label">Overall</div>
      </div>
    </div>
    <div class="card">
      <h3 class="mb-2">Evaluation Timeline</h3>
      <p class="text-secondary" style="font-size:0.85rem;">No evaluations yet. Start by evaluating responses from the Batch Evaluation tab.</p>
    </div>`;
}

function loadBatchResponses() {
  const tbody = document.getElementById('eval-batch-tbody');
  if (!tbody) return;

  const examFilter = document.getElementById('eval-filter-exam')?.value || '';
  const statusFilter = document.getElementById('eval-filter-status')?.value || '';

  // Mock data - replace with actual API call
  const mockResponses = [
    { id: 1, studentName: 'Rajesh Kumar', exam: 'GATE', submitted: '2 hours ago', status: 'submitted', score: null },
    { id: 2, studentName: 'Priya Singh', exam: 'SSC CGL', submitted: '5 hours ago', status: 'submitted', score: null },
    { id: 3, studentName: 'Amit Patel', exam: 'CAT', submitted: '1 day ago', status: 'in-progress', score: null },
    { id: 4, studentName: 'Neha Sharma', exam: 'GATE', submitted: '2 days ago', status: 'evaluated', score: '78/100' },
  ];

  const filtered = mockResponses.filter(r => {
    return (!examFilter || r.exam.toLowerCase().includes(examFilter.toLowerCase())) &&
           (!statusFilter || r.status === statusFilter);
  });

  if (filtered.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="5" style="text-align:center; padding:40px;">
          <div class="empty-state">
            <div class="icon">📋</div>
            <p>No responses match the selected filters</p>
          </div>
        </td>
      </tr>`;
    return;
  }

  tbody.innerHTML = filtered.map(r => `
    <tr>
      <td><strong>${r.studentName}</strong></td>
      <td>${r.exam}</td>
      <td style="font-size:0.85rem; color:var(--text-secondary);">${r.submitted}</td>
      <td>
        <span class="eval-status-badge eval-status-${r.status}">
          ${r.status === 'submitted' ? '✓ Submitted' : r.status === 'in-progress' ? '⏳ In Progress' : '✅ Evaluated'}
        </span>
      </td>
      <td>
        ${r.status === 'evaluated'
          ? `<button class="btn btn-secondary btn-sm" onclick="viewEvaluation(${r.id})">View</button>`
          : `<button class="btn btn-primary btn-sm" onclick="startEvaluation(${r.id})">Evaluate</button>`}
      </td>
    </tr>`).join('');
}

function startEvaluation(responseId) {
  state.currentEvaluation = {
    id: responseId,
    studentName: 'Rajesh Kumar',
    exam: 'GATE',
    currentQuestion: 1,
    totalQuestions: 5,
    responses: {
      1: { studentAnswer: 'A', correctAnswer: 'A', isCorrect: true, marks: 2 },
      2: { studentAnswer: 'C', correctAnswer: 'B', isCorrect: false, marks: 0 },
      3: { studentAnswer: null, correctAnswer: 'D', isCorrect: false, marks: 0 },
      4: { studentAnswer: 'B', correctAnswer: 'B', isCorrect: true, marks: 2 },
      5: { studentAnswer: 'A', correctAnswer: 'A', isCorrect: true, marks: 2 },
    },
  };
  showEvaluationModal();
}

function viewEvaluation(responseId) {
  toast('Opening evaluation details...', 'info');
}

function showEvaluationModal() {
  const eval = state.currentEvaluation;
  const q = eval.currentQuestion;
  const data = eval.responses[q];

  const modalContent = `
    <div class="eval-modal">
      <div class="modal-header">
        <div>
          <h2>${eval.studentName}</h2>
          <p class="text-secondary" style="font-size:0.85rem;">${eval.exam} Evaluation</p>
        </div>
        <div style="text-align:right;">
          <div class="progress-info" style="flex-direction: column; margin: 0; padding: 0; border: none; gap: 8px;">
            <span class="badge badge-info">Question ${q} of ${eval.totalQuestions}</span>
            <div class="progress-bar" style="width:100px;">
              <div class="progress-fill good" style="width:${(q/eval.totalQuestions)*100}%"></div>
            </div>
          </div>
        </div>
      </div>

      <div class="question-block">
        <div class="question-header">
          <span class="question-number">Q${q}. MCQ (Single Correct Answer)</span>
          <span class="question-progress" style="color:var(--accent-light);">+${data.isCorrect ? '2' : '0'} marks</span>
        </div>

        <div class="question-text">
          What is the time complexity of binary search in a sorted array?
        </div>

        <div class="options-group">
          <label class="option-item ${data.studentAnswer === 'A' ? 'selected' : ''}">
            <input type="radio" name="answer" value="A" ${data.studentAnswer === 'A' ? 'checked' : ''} disabled>
            <span><strong>A.</strong> O(1)</span>
            ${data.studentAnswer === 'A' ? '<span style="margin-left:auto; color:var(--text-secondary);">Student Answer</span>' : ''}
          </label>
          <label class="option-item ${data.correctAnswer === 'B' ? 'selected' : ''}">
            <input type="radio" name="answer" value="B" ${data.correctAnswer === 'B' ? 'checked' : ''} disabled>
            <span><strong>B.</strong> O(log n)</span>
            ${data.correctAnswer === 'B' ? '<span style="margin-left:auto; color:var(--success);">Correct Answer</span>' : ''}
          </label>
          <label class="option-item">
            <input type="radio" name="answer" value="C" disabled>
            <span><strong>C.</strong> O(n)</span>
          </label>
          <label class="option-item">
            <input type="radio" name="answer" value="D" disabled>
            <span><strong>D.</strong> O(n log n)</span>
          </label>
        </div>

        ${!data.isCorrect ? `
          <div class="mt-2">
            <div class="answer-display incorrect">
              <strong style="color:var(--danger);">✗ Incorrect</strong> - Student answered: ${data.studentAnswer}
            </div>
          </div>
        ` : `
          <div class="mt-2">
            <div class="answer-display correct">
              <strong style="color:var(--success);">✓ Correct</strong> - Well done!
            </div>
          </div>
        `}

        <div style="margin-top:24px; padding-top:16px; border-top:1px solid var(--border);">
          <h4 style="margin-bottom:8px; color:var(--text-secondary);">Evaluator Notes</h4>
          <textarea class="form-input" id="eval-notes" placeholder="Add any notes about this question..." rows="3"></textarea>
        </div>
      </div>

      <div class="eval-modal .progress-info">
        <button class="btn btn-secondary" onclick="closeEvaluationModal()">Cancel</button>
        <div style="margin-left:auto; display:flex; gap:12px;">
          <button class="btn btn-secondary" onclick="previousQuestion()" ${q === 1 ? 'disabled' : ''}>← Previous</button>
          <button class="btn btn-primary" onclick="nextQuestion()" ${q === eval.totalQuestions ? 'disabled' : ''}>Next →</button>
          ${q === eval.totalQuestions ? '<button class="btn btn-success" onclick="submitEvaluation()">✓ Submit Evaluation</button>' : ''}
        </div>
      </div>
    </div>`;

  const overlay = document.createElement('div');
  overlay.className = 'modal-overlay';
  overlay.id = 'eval-modal-overlay';
  overlay.onclick = (e) => { if (e.target === overlay) closeEvaluationModal(); };
  overlay.innerHTML = `<div class="modal fade-in">${modalContent}</div>`;
  document.body.appendChild(overlay);
}

function previousQuestion() {
  if (state.currentEvaluation.currentQuestion > 1) {
    state.currentEvaluation.currentQuestion--;
    closeEvaluationModal();
    showEvaluationModal();
  }
}

function nextQuestion() {
  if (state.currentEvaluation.currentQuestion < state.currentEvaluation.totalQuestions) {
    state.currentEvaluation.currentQuestion++;
    closeEvaluationModal();
    showEvaluationModal();
  }
}

function submitEvaluation() {
  const overlay = document.getElementById('eval-modal-overlay');
  if (overlay) {
    overlay.style.animation = 'fadeOut 0.3s forwards';
    setTimeout(() => overlay.remove(), 300);
  }
  toast('Evaluation submitted successfully!', 'success');
  state.currentEvaluation = null;
  // Reload batch responses
  setTimeout(() => loadBatchResponses(), 500);
}

function closeEvaluationModal() {
  const overlay = document.getElementById('eval-modal-overlay');
  if (overlay) {
    overlay.style.animation = 'fadeOut 0.3s forwards';
    setTimeout(() => overlay.remove(), 300);
  }
}

function evaluatorLogout() {
  state.adminToken = null;
  state.userRole = null;
  localStorage.removeItem('evalx_admin_token');
  localStorage.removeItem('evalx_user_role');
  updateRoleIndicator();
  updateNavigation();
  toast('Logged out', 'success');
  navigate('home');
}

// ── Student Panel ──────────────────────────────────────────────────────────

function renderStudent() {
  return `
    <div class="main-layout">
      <aside class="main-sidebar">
        <div class="sidebar-section">
          <div class="sidebar-label">📚 My Exams</div>
          <button class="sidebar-item active" onclick="switchStudentView('upload')">
            <span>📤</span> Upload Response
          </button>
          <button class="sidebar-item" onclick="switchStudentView('results')">
            <span>📊</span> View Results
          </button>
        </div>
        <div style="margin-top: auto; padding-top: 24px;">
          <button class="sidebar-item" style="color: var(--danger)" onclick="studentLogout()">
            <span>🚪</span> Logout
          </button>
        </div>
      </aside>
      <main class="main-content fade-in">
        <h2>Student Dashboard</h2>
        <p class="text-secondary mb-3">Upload your response sheet and view your evaluation results</p>
        <div id="student-main">
          ${renderStudentUpload()}
        </div>
      </main>
    </div>`;
}

function renderStudentUpload() {
  return `
    <div class="card premium-card fade-in">
      <div class="flex items-center gap-2 mb-2">
        <span class="badge badge-success">Instant Evaluation</span>
        <h3>Upload Your Response Sheet</h3>
      </div>
      <p class="text-secondary mb-3">Upload your completed response sheet to get instant evaluation and results</p>

      <div class="upload-zone" id="student-upload-zone"
           ondragover="event.preventDefault(); this.classList.add('dragover')"
           ondragleave="this.classList.remove('dragover')"
           ondrop="handleStudentDrop(event)"
           onclick="document.getElementById('student-file-input').click()">
        <input type="file" id="student-file-input" accept=".pdf,.csv,.json" onchange="handleStudentFile(event)">
        <div class="icon">📄</div>
        <h3>Drop your response sheet here</h3>
        <p class="text-muted">Supports PDF, CSV, and JSON</p>
        <div id="student-file-info" class="file-info hidden"></div>
      </div>
      <button class="btn btn-primary btn-lg mt-3 w-full justify-center" id="student-upload-btn" onclick="uploadStudentResponse()" disabled>
        ⚡ Evaluate My Answers
      </button>
    </div>`;
}

let studentFile = null;

function handleStudentDrop(e) {
  e.preventDefault();
  e.currentTarget.classList.remove('dragover');
  studentFile = e.dataTransfer.files[0];
  showStudentFileInfo();
}

function handleStudentFile(e) {
  studentFile = e.target.files[0];
  showStudentFileInfo();
}

function showStudentFileInfo() {
  if (!studentFile) return;
  const info = document.getElementById('student-file-info');
  info.textContent = `✓ ${studentFile.name}`;
  info.classList.remove('hidden');
  document.getElementById('student-upload-btn').disabled = false;
}

async function uploadStudentResponse() {
  if (!studentFile) return;
  state.loading = true;
  document.getElementById('student-upload-btn').disabled = true;
  document.getElementById('student-upload-btn').textContent = 'Evaluating...';

  try {
    const formData = new FormData();
    formData.append('file', studentFile);
    const result = await api('POST', '/api/evaluation/evaluate', formData, true);
    state.result = result;
    navigate('result');
    toast('Evaluation Complete!', 'success');
  } catch (e) {
    toast('Evaluation Failed: ' + e.message, 'error');
    state.loading = false;
    document.getElementById('student-upload-btn').disabled = false;
    document.getElementById('student-upload-btn').textContent = '⚡ Evaluate My Answers';
  }
}

function switchStudentView(view) {
  const main = document.getElementById('student-main');
  if (view === 'upload' && main) {
    main.innerHTML = renderStudentUpload();
  } else if (view === 'results' && main) {
    main.innerHTML = renderStudentResults();
  }
  document.querySelectorAll('.main-sidebar .sidebar-item').forEach(b => b.classList.remove('active'));
  event?.target?.closest('.sidebar-item')?.classList.add('active');
}

function renderStudentResults() {
  // Mock student results
  const mockResults = [
    {
      id: 1,
      exam: 'GATE 2024',
      submitted: '2 hours ago',
      status: 'evaluated',
      score: '78/100',
      accuracy: '78%',
      correct: 39,
      incorrect: 10,
      skipped: 1
    },
    {
      id: 2,
      exam: 'SSC CGL 2024',
      submitted: '1 day ago',
      status: 'evaluating',
      score: null,
      accuracy: null,
      correct: null,
      incorrect: null,
      skipped: null
    },
  ];

  return `
    <div class="card premium-card mb-3">
      <h3 class="mb-1">Your Evaluation Results</h3>
      <p class="text-secondary" style="font-size:0.85rem;">Track your submissions and view detailed evaluation results</p>
    </div>
    <div class="card mb-3">
      <table class="evaluation-table">
        <thead>
          <tr>
            <th>Exam</th>
            <th>Submitted</th>
            <th>Status</th>
            <th>Score</th>
            <th>Accuracy</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          ${mockResults.map(r => `
            <tr>
              <td><strong>${r.exam}</strong></td>
              <td style="font-size:0.85rem; color:var(--text-secondary);">${r.submitted}</td>
              <td>
                <span class="eval-status-badge ${r.status === 'evaluated' ? 'eval-status-evaluated' : 'eval-status-pending'}">
                  ${r.status === 'evaluated' ? '✅ Evaluated' : '⏳ Evaluating'}
                </span>
              </td>
              <td>${r.score ? `<strong>${r.score}</strong>` : '—'}</td>
              <td>${r.accuracy ? `<strong>${r.accuracy}</strong>` : '—'}</td>
              <td>
                ${r.status === 'evaluated'
                  ? `<button class="btn btn-primary btn-sm" onclick="viewStudentResults(${r.id})">View Details</button>`
                  : `<span class="text-muted" style="font-size:0.85rem;">In progress...</span>`}
              </td>
            </tr>`).join('')}
        </tbody>
      </table>
    </div>
    <div class="card">
      <h3 class="mb-2">Quick Stats</h3>
      <div class="grid grid-3">
        <div style="text-align:center; padding:16px;">
          <div style="font-size:1.5rem; font-weight:800; color:var(--success);">2</div>
          <div class="text-muted" style="font-size:0.8rem;">Total Submissions</div>
        </div>
        <div style="text-align:center; padding:16px;">
          <div style="font-size:1.5rem; font-weight:800; color:var(--accent-light);">1</div>
          <div class="text-muted" style="font-size:0.8rem;">Evaluated</div>
        </div>
        <div style="text-align:center; padding:16px;">
          <div style="font-size:1.5rem; font-weight:800; color:var(--info);">78%</div>
          <div class="text-muted" style="font-size:0.8rem;">Best Accuracy</div>
        </div>
      </div>
    </div>`;
}

function viewStudentResults(resultId) {
  // Mock detailed result
  const result = {
    exam: 'GATE 2024',
    score: 78,
    maxScore: 100,
    correct: 39,
    incorrect: 10,
    skipped: 1,
    accuracy: 78,
    percentile: 85,
    estimatedRank: 15000,
    sections: [
      { name: 'Data Structures', score: 28, maxScore: 40, accuracy: 70 },
      { name: 'Algorithms', score: 30, maxScore: 40, accuracy: 75 },
      { name: 'Database', score: 20, maxScore: 20, accuracy: 100 },
    ]
  };

  const modal = `
    <div style="max-width:700px;">
      <div class="result-hero" style="margin: -32px -32px 32px -32px; padding: 32px 32px 24px; background: var(--bg-glass);">
        <p class="text-secondary">${result.exam}</p>
        <div class="score-display text-gradient" style="margin: 16px 0 8px;">${result.score} / ${result.maxScore}</div>
        <p class="score-sub">Your Score</p>
      </div>

      <div class="grid grid-3 mb-3">
        <div class="stat-card success" style="padding:16px; text-align:center;">
          <div class="stat-value">${result.correct}</div>
          <div class="stat-label">Correct</div>
        </div>
        <div class="stat-card danger" style="padding:16px; text-align:center;">
          <div class="stat-value">${result.incorrect}</div>
          <div class="stat-label">Incorrect</div>
        </div>
        <div class="stat-card warning" style="padding:16px; text-align:center;">
          <div class="stat-value">${result.skipped}</div>
          <div class="stat-label">Skipped</div>
        </div>
      </div>

      <div class="card mb-3">
        <h3 class="mb-2">Performance Metrics</h3>
        <table class="results-table" style="width:100%;">
          <tr><td class="text-secondary">Accuracy</td><td style="font-weight:600; text-align:right;">${result.accuracy}%</td></tr>
          <tr><td class="text-secondary">Predicted Percentile</td><td style="font-weight:600; text-align:right; color:var(--accent-light);">${result.percentile}%</td></tr>
          <tr><td class="text-secondary">Estimated Rank</td><td style="font-weight:600; text-align:right; color:var(--accent-light);">~${result.estimatedRank.toLocaleString()}</td></tr>
        </table>
      </div>

      <div class="card">
        <h3 class="mb-2">Section-wise Performance</h3>
        <table class="results-table" style="width:100%;">
          <thead><tr><th>Section</th><th>Score</th><th>Accuracy</th></tr></thead>
          <tbody>
            ${result.sections.map(s => `
              <tr>
                <td><strong>${s.name}</strong></td>
                <td>${s.score}/${s.maxScore}</td>
                <td>
                  <div class="flex items-center gap-1">
                    <span>${s.accuracy}%</span>
                    <div class="progress-bar" style="width:60px; height:4px;">
                      <div class="progress-fill good" style="width:${s.accuracy}%"></div>
                    </div>
                  </div>
                </td>
              </tr>`).join('')}
          </tbody>
        </table>
      </div>
    </div>`;

  showModal(`${result.exam} - Detailed Results`, modal, () => closeModal());
}

function studentLogout() {
  state.userRole = null;
  localStorage.removeItem('evalx_user_role');
  updateRoleIndicator();
  updateNavigation();
  toast('Logged out', 'success');
  navigate('home');
}

// ── Admin Panel ────────────────────────────────────────────────────────────

function renderAdminLogin() {
  return `
    <div class="page flex items-center justify-center fade-in" style="min-height: calc(100vh - 72px)">
      <div class="card" style="width: 100%; max-width: 400px;">
        <h2 class="mb-3 text-center" style="text-align:center;">Admin Login</h2>
        <div class="form-group">
          <label>Username</label>
          <input class="form-input" id="admin-user" type="text" placeholder="admin">
        </div>
        <div class="form-group mb-4">
          <label>Password</label>
          <input class="form-input" id="admin-pass" type="password" placeholder="admin123">
        </div>
        <button class="btn btn-primary btn-lg w-full justify-center" onclick="adminLogin()">Login to Dashboard</button>
      </div>
    </div>`;
}

async function adminLogin() {
  const user = document.getElementById('admin-user').value;
  const pass = document.getElementById('admin-pass').value;
  if (!user || !pass) return toast('Please enter username and password', 'error');
  try {
    const res = await api('POST', '/api/auth/login', { username: user, password: pass });
    state.adminToken = res.token;
    localStorage.setItem('evalx_admin_token', res.token);
    setUserRole('admin');
    toast('Login successful!', 'success');
    navigate('admin');
  } catch (e) { toast(e.message, 'error'); }
}

function adminLogout() {
  state.adminToken = null;
  state.userRole = null;
  localStorage.removeItem('evalx_admin_token');
  localStorage.removeItem('evalx_user_role');
  updateRoleIndicator();
  updateNavigation();
  toast('Logged out', 'success');
  navigate('admin');
}

function renderAdmin() {
  if (!state.adminToken) return renderAdminLogin();
  return `
    <div class="admin-layout">
      <aside class="admin-sidebar">
        <div class="menu-label">Main</div>
        <button class="menu-item ${state.adminSection === 'magic-ingest' ? 'active' : ''}" onclick="adminNav('magic-ingest')">
          <span>🚀</span> Magic PDF Ingest
        </button>
        <button class="menu-item ${state.adminSection === 'dashboard' ? 'active' : ''}" onclick="adminNav('dashboard')">
          <span>📊</span> Stats
        </button>
        
        <div class="menu-label flex justify-between items-center" style="cursor:pointer" onclick="toggleAdminAdvanced()">
          <span>⚙️ Advanced Settings</span>
          <span>${state.adminAdvancedOpen ? '▾' : '▸'}</span>
        </div>
        
        <div id="admin-advanced-menu" class="${state.adminAdvancedOpen ? '' : 'hidden'}">
          ${['exams', 'stages', 'sections', 'questions', 'marking', 'candidates'].map(s =>
    `<button class="menu-item ${state.adminSection === s ? 'active' : ''}" onclick="adminNav('${s}')">
              <span>${{ 'exams': '📋', 'stages': '📑', 'sections': '📁', 'questions': '❓', 'marking': '⚖️', 'candidates': '👥' }[s]}</span>
              ${s.split('-').map(w => w[0].toUpperCase() + w.slice(1)).join(' ')}
            </button>`
  ).join('')}
        </div>
        
        <div style="margin-top:auto; padding-top:24px;">
           <button class="menu-item" style="color:var(--danger)" onclick="adminLogout()"><span>🚪</span> Logout</button>
        </div>
      </aside>
      <main class="admin-content fade-in" id="admin-main">
        ${renderAdminSection()}
      </main>
    </div>`;
}

function adminNav(section) {
  state.adminSection = section;
  const main = document.getElementById('admin-main');
  if (main) { main.innerHTML = renderAdminSection(); }
}

function renderAdminSection() {
  switch (state.adminSection) {
    case 'magic-ingest': return renderAdminPdfIngest();
    case 'dashboard': return renderAdminDashboard();
    case 'exams': return renderAdminExams();
    case 'stages': return renderAdminStages();
    case 'sections': return renderAdminSections();
    case 'questions': return renderAdminQuestions();
    case 'marking': return renderAdminMarking();
    case 'candidates': return renderAdminCandidates();
    default: return renderAdminPdfIngest();
  }
}

function toggleAdminAdvanced() {
  state.adminAdvancedOpen = !state.adminAdvancedOpen;
  render();
}

function renderAdminPdfIngest() {
  return `
    <div class="hero-mini fade-in">
       <h1>✨ Magic <span class="text-gradient">PDF Ingest</span></h1>
       <p>Automate your entire exam setup by uploading master documents. No manual forms needed.</p>
    </div>
    <div class="card premium-card mb-4 fade-in" style="max-width: 800px; margin: 0 auto;">
      <div class="flex items-center gap-2 mb-3">
        <span class="badge badge-success">V3 Magic</span>
        <h3>Zero-Friction Exam Seeding</h3>
      </div>
      
      <div class="grid grid-2 gap-4 mt-2">
        <div class="form-group">
          <label>1. Master Question Paper PDF</label>
          <div class="upload-zone" style="padding: 40px 20px;" onclick="document.getElementById('main-ingest-qp').click()">
            <input type="file" id="main-ingest-qp" accept=".pdf" style="display:none" onchange="this.parentElement.querySelector('small').textContent = '✓ ' + this.files[0].name">
            <div class="icon" style="font-size:2rem">📄</div>
            <small class="text-muted">Select Master Paper...</small>
          </div>
        </div>
        <div class="form-group">
          <label>2. Official Answer Key PDF</label>
          <div class="upload-zone" style="padding: 40px 20px;" onclick="document.getElementById('main-ingest-ak').click()">
            <input type="file" id="main-ingest-ak" accept=".pdf" style="display:none" onchange="this.parentElement.querySelector('small').textContent = '✓ ' + this.files[0].name">
            <div class="icon" style="font-size:2rem">🔑</div>
            <small class="text-muted">Select Answer Key...</small>
          </div>
        </div>
      </div>
      
      <button class="btn btn-primary btn-lg mt-4 w-100 justify-center" id="main-ingest-btn" onclick="ingestPdfsDirectly()">🚀 Process & Auto-Create Exam</button>
      
      <p class="text-muted mt-3 text-center" style="font-size:0.8rem;">
        Our engine will auto-detect the <b>Exam (GATE/SSC/CAT)</b>, <b>Year</b>, and <b>Shift</b> from the PDF headers and create the entire structure for you.
      </p>
    </div>`;
}

async function ingestPdfsDirectly() {
  const qpFile = document.getElementById('main-ingest-qp').files[0];
  const akFile = document.getElementById('main-ingest-ak').files[0];

  if (!qpFile || !akFile) return toast('Both PDF files are required', 'error');

  const btn = document.getElementById('main-ingest-btn');
  btn.disabled = true; btn.textContent = 'Parsing PDFs & Creating Hierarchy...';

  try {
    const formData = new FormData();
    formData.append('questionPaper', qpFile);
    formData.append('answerKey', akFile);
    await api('POST', `/api/admin/ingest`, formData, true);
    toast('Magic! Exam hierarchy created and seeded successfully.', 'success');
  } catch (e) {
    toast('Error: ' + e.message, 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = '🚀 Process & Auto-Create Exam';
  }
}

function renderAdminDashboard() {
  loadAdminExams();
  return `
    <div class="fade-in">
      <div style="margin-bottom:32px;">
        <h2 class="mb-1">Admin Dashboard</h2>
        <p class="text-secondary">System overview and evaluation metrics</p>
      </div>

      <!-- Key Metrics -->
      <div class="grid grid-4 mb-4">
        <div class="card stat-card accent">
          <div class="stat-value" id="admin-exam-count">—</div>
          <div class="stat-label">Active Exams</div>
        </div>
        <div class="card stat-card info">
          <div class="stat-value">12</div>
          <div class="stat-label">Evaluators</div>
        </div>
        <div class="card stat-card success">
          <div class="stat-value">347</div>
          <div class="stat-label">Responses</div>
        </div>
        <div class="card stat-card warning">
          <div class="stat-value">73.2%</div>
          <div class="stat-label">Evaluated</div>
        </div>
      </div>

      <!-- Overview Cards -->
      <div class="grid grid-2 mb-4">
        <div class="card">
          <h3 class="mb-3">Evaluation Status</h3>
          <div style="display:flex; flex-direction:column; gap:16px;">
            <div>
              <div style="display:flex; justify-content:space-between; margin-bottom:8px;">
                <span class="text-secondary" style="font-size:0.85rem;">Completed</span>
                <span style="font-weight:600; color:var(--success);">254 (73%)</span>
              </div>
              <div class="progress-bar">
                <div class="progress-fill good" style="width:73%"></div>
              </div>
            </div>
            <div>
              <div style="display:flex; justify-content:space-between; margin-bottom:8px;">
                <span class="text-secondary" style="font-size:0.85rem;">In Progress</span>
                <span style="font-weight:600; color:var(--info);">65 (19%)</span>
              </div>
              <div class="progress-bar">
                <div class="progress-fill" style="background:var(--info); width:19%"></div>
              </div>
            </div>
            <div>
              <div style="display:flex; justify-content:space-between; margin-bottom:8px;">
                <span class="text-secondary" style="font-size:0.85rem;">Pending</span>
                <span style="font-weight:600; color:var(--warning);">28 (8%)</span>
              </div>
              <div class="progress-bar">
                <div class="progress-fill avg" style="width:8%"></div>
              </div>
            </div>
          </div>
        </div>

        <div class="card">
          <h3 class="mb-3">Performance Snapshot</h3>
          <table class="results-table" style="margin:0;">
            <tr>
              <td class="text-secondary" style="font-size:0.85rem;">Average Score</td>
              <td style="text-align:right; font-weight:600; color:var(--accent-light);">72.4/100</td>
            </tr>
            <tr>
              <td class="text-secondary" style="font-size:0.85rem;">Highest Score</td>
              <td style="text-align:right; font-weight:600; color:var(--success);">98/100</td>
            </tr>
            <tr>
              <td class="text-secondary" style="font-size:0.85rem;">Lowest Score</td>
              <td style="text-align:right; font-weight:600; color:var(--danger);">15/100</td>
            </tr>
            <tr>
              <td class="text-secondary" style="font-size:0.85rem;">Median Score</td>
              <td style="text-align:right; font-weight:600;">71/100</td>
            </tr>
          </table>
        </div>
      </div>

      <!-- Quick Actions -->
      <div class="card premium-card mb-4">
        <h3 class="mb-3">⚡ Quick Setup</h3>
        <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap:16px;">
          <button onclick="adminNav('exams')" style="padding:16px; background:var(--bg-glass); border:1px solid var(--border); border-radius:10px; cursor:pointer; transition:all 0.2s; text-align:left; color:var(--text-primary);" onmouseover="this.style.borderColor='var(--border-active)'" onmouseout="this.style.borderColor='var(--border)'">
            <div style="font-weight:600; margin-bottom:4px;">📋 Create Exam</div>
            <div style="font-size:0.8rem; color:var(--text-secondary);">Set up a new exam</div>
          </button>
          <button onclick="adminNav('stages')" style="padding:16px; background:var(--bg-glass); border:1px solid var(--border); border-radius:10px; cursor:pointer; transition:all 0.2s; text-align:left; color:var(--text-primary);" onmouseover="this.style.borderColor='var(--border-active)'" onmouseout="this.style.borderColor='var(--border)'">
            <div style="font-weight:600; margin-bottom:4px;">📑 Add Stages</div>
            <div style="font-size:0.8rem; color:var(--text-secondary);">Create exam stages</div>
          </button>
          <button onclick="adminNav('questions')" style="padding:16px; background:var(--bg-glass); border:1px solid var(--border); border-radius:10px; cursor:pointer; transition:all 0.2s; text-align:left; color:var(--text-primary);" onmouseover="this.style.borderColor='var(--border-active)'" onmouseout="this.style.borderColor='var(--border)'">
            <div style="font-weight:600; margin-bottom:4px;">❓ Upload Questions</div>
            <div style="font-size:0.8rem; color:var(--text-secondary);">Seed questions & answers</div>
          </button>
          <button onclick="adminNav('magic-ingest')" style="padding:16px; background:var(--bg-glass); border:1px solid var(--border); border-radius:10px; cursor:pointer; transition:all 0.2s; text-align:left; color:var(--text-primary);" onmouseover="this.style.borderColor='var(--border-active)'" onmouseout="this.style.borderColor='var(--border)'">
            <div style="font-weight:600; margin-bottom:4px;">🚀 Magic PDF Ingest</div>
            <div style="font-size:0.8rem; color:var(--text-secondary);">Auto-ingest from PDFs</div>
          </button>
          <button onclick="adminNav('marking')" style="padding:16px; background:var(--bg-glass); border:1px solid var(--border); border-radius:10px; cursor:pointer; transition:all 0.2s; text-align:left; color:var(--text-primary);" onmouseover="this.style.borderColor='var(--border-active)'" onmouseout="this.style.borderColor='var(--border)'">
            <div style="font-weight:600; margin-bottom:4px;">⚖️ Marking Scheme</div>
            <div style="font-size:0.8rem; color:var(--text-secondary);">Set marking rules</div>
          </button>
          <button onclick="adminNav('candidates')" style="padding:16px; background:var(--bg-glass); border:1px solid var(--border); border-radius:10px; cursor:pointer; transition:all 0.2s; text-align:left; color:var(--text-primary);" onmouseover="this.style.borderColor='var(--border-active)'" onmouseout="this.style.borderColor='var(--border)'">
            <div style="font-weight:600; margin-bottom:4px;">👥 Candidate Count</div>
            <div style="font-size:0.8rem; color:var(--text-secondary);">Update candidate pool</div>
          </button>
        </div>
      </div>

      <!-- Recent Activity -->
      <div class="card">
        <h3 class="mb-2">📊 Recent Evaluations</h3>
        <table class="results-table">
          <thead>
            <tr>
              <th>Student</th>
              <th>Exam</th>
              <th>Evaluator</th>
              <th>Time</th>
              <th>Score</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><strong>Rajesh Kumar</strong></td>
              <td>GATE 2024</td>
              <td>Priya</td>
              <td style="font-size:0.85rem; color:var(--text-secondary);">5 min ago</td>
              <td style="font-weight:600; color:var(--success);">82/100</td>
            </tr>
            <tr>
              <td><strong>Amit Patel</strong></td>
              <td>SSC CGL</td>
              <td>Arjun</td>
              <td style="font-size:0.85rem; color:var(--text-secondary);">12 min ago</td>
              <td style="font-weight:600; color:var(--success);">71/100</td>
            </tr>
            <tr>
              <td><strong>Neha Sharma</strong></td>
              <td>CAT 2024</td>
              <td>Priya</td>
              <td style="font-size:0.85rem; color:var(--text-secondary);">28 min ago</td>
              <td style="font-weight:600; color:var(--danger);">45/100</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>`;
}

// ── Admin CRUD Panels ──────────────────────────────────────────────────────

// EXAMS
function renderAdminExams() {
  loadAdminExams();
  return `
    <div class="flex justify-between items-center mb-3">
      <h2>Manage Exams</h2>
      <button class="btn btn-primary" onclick="showCreateExamModal()">+ Create Exam</button>
    </div>
    <div id="admin-exams-list"><div class="loading-overlay"><div class="spinner"></div></div></div>`;
}

async function loadAdminExams() {
  try {
    state.adminExams = await api('GET', '/api/exams');
    const el = document.getElementById('admin-exams-list');
    if (el) {
      el.innerHTML = state.adminExams.length === 0 ? '<div class="empty-state"><div class="icon">📋</div><p>No exams yet. Create your first exam.</p></div>' :
        '<div class="grid grid-2">' + state.adminExams.map(e => `
          <div class="card">
            <div class="flex justify-between items-center mb-2">
              <h3>${e.name}</h3>
              <button class="btn btn-danger btn-sm" onclick="deleteExam(${e.id})">Delete</button>
            </div>
            <p class="text-secondary" style="font-size:0.85rem;">Code: ${e.code} • ${e.stages?.length || 0} stages</p>
            <p class="text-muted" style="font-size:0.8rem;margin-top:4px;">${e.description || ''}</p>
          </div>`).join('') + '</div>';
    }
    // Update dashboard counts
    const ec = document.getElementById('admin-exam-count');
    if (ec) ec.textContent = state.adminExams.length;
    let totalStages = state.adminExams.reduce((a, e) => a + (e.stages?.length || 0), 0);
    const sc = document.getElementById('admin-stage-count');
    if (sc) sc.textContent = totalStages;
  } catch (e) { console.error(e); }
}

function showCreateExamModal() {
  showModal('Create Exam', `
    <div class="form-group"><label>Exam Name</label><input class="form-input" id="modal-exam-name" placeholder="e.g., SSC CGL"></div>
    <div class="form-group"><label>Code</label><input class="form-input" id="modal-exam-code" placeholder="e.g., SSC-CGL"></div>
    <div class="form-group"><label>Description</label><input class="form-input" id="modal-exam-desc" placeholder="Short description"></div>
  `, async () => {
    const name = document.getElementById('modal-exam-name').value;
    const code = document.getElementById('modal-exam-code').value;
    const desc = document.getElementById('modal-exam-desc').value;
    if (!name || !code) { toast('Name and Code are required', 'error'); return; }
    await api('POST', '/api/exams', { name, code, description: desc });
    toast('Exam created!', 'success');
    closeModal(); state.adminExams = []; state.exams = []; loadAdminExams();
  });
}

async function deleteExam(id) {
  if (!confirm('Delete this exam and all its data?')) return;
  try { await api('DELETE', `/api/exams/${id}`); toast('Exam deleted', 'success'); state.adminExams = []; state.exams = []; loadAdminExams(); }
  catch (e) { toast('Error: ' + e.message, 'error'); }
}

// STAGES
function renderAdminStages() {
  loadAdminExams();
  return `
    <div class="flex justify-between items-center mb-3">
      <h2>Manage Stages</h2>
      <button class="btn btn-primary" onclick="showCreateStageModal()">+ Create Stage</button>
    </div>
    <div id="admin-stages-list"><div class="loading-overlay"><div class="spinner"></div></div></div>`;
}

function showCreateStageModal() {
  showModal('Create Stage', `
    <div class="form-group"><label>Exam</label>
      <select class="form-input" id="modal-stage-exam">
        ${state.adminExams.map(e => `<option value="${e.id}">${e.name}</option>`).join('')}
      </select>
    </div>
    <div class="form-group"><label>Stage Name</label><input class="form-input" id="modal-stage-name" placeholder="e.g., Pre (Tier 1)"></div>
    <div class="form-group"><label>Description</label><input class="form-input" id="modal-stage-desc" placeholder="Description"></div>
    <div class="form-group"><label>Order</label><input class="form-input" id="modal-stage-order" type="number" value="1"></div>
  `, async () => {
    const examId = +document.getElementById('modal-stage-exam').value;
    const name = document.getElementById('modal-stage-name').value;
    const desc = document.getElementById('modal-stage-desc').value;
    const order = +document.getElementById('modal-stage-order').value;
    if (!name) { toast('Stage name required', 'error'); return; }
    await api('POST', '/api/exam-stages', { examId, name, description: desc, orderIndex: order });
    toast('Stage created!', 'success');
    closeModal(); state.adminExams = []; state.exams = []; adminNav('stages');
  });
}

// SECTIONS
function renderAdminSections() {
  return `
    <div class="flex justify-between items-center mb-3">
      <h2>Manage Sections</h2>
      <button class="btn btn-primary" onclick="showCreateSectionModal()">+ Create Section</button>
    </div>
    <div class="form-group">
      <label>Select Exam Year</label>
      <select class="form-input" id="admin-section-ey" onchange="loadAdminSections()">
        <option value="">— Choose —</option>
      </select>
    </div>
    <div id="admin-sections-list"></div>`;
}

async function showCreateSectionModal() {
  showModal('Create Section', `
    <div class="form-group"><label>Exam Year ID</label><input class="form-input" id="modal-sec-eyid" type="number" placeholder="Exam Year ID"></div>
    <div class="form-group"><label>Section Name</label><input class="form-input" id="modal-sec-name" placeholder="e.g., General Intelligence"></div>
    <div class="form-group"><label>Total Questions</label><input class="form-input" id="modal-sec-total" type="number" value="25"></div>
    <div class="form-group"><label>Order</label><input class="form-input" id="modal-sec-order" type="number" value="1"></div>
  `, async () => {
    const eyId = +document.getElementById('modal-sec-eyid').value;
    const name = document.getElementById('modal-sec-name').value;
    const total = +document.getElementById('modal-sec-total').value;
    const order = +document.getElementById('modal-sec-order').value;
    if (!eyId || !name) { toast('Exam Year ID and Name required', 'error'); return; }
    await api('POST', '/api/sections', { examYearId: eyId, name, totalQuestions: total, orderIndex: order });
    toast('Section created!', 'success');
    closeModal();
  });
}

// QUESTIONS
function renderAdminQuestions() {
  return `
    <h2 class="mb-3">Questions & Answer Keys</h2>
    
    <div class="tabs mb-2">
      <button class="tab-btn active" onclick="switchQuestionTab('bulk')">CSV Bulk Upload</button>
      <button class="tab-btn" onclick="switchQuestionTab('ingest')">Magic PDF Ingest (V2)</button>
    </div>

    <div id="question-tab-bulk" class="card mb-3">
      <h3 class="mb-2">Bulk Upload via CSV</h3>
      <p class="text-secondary mb-2" style="font-size:0.85rem;">CSV format: <code>questionNumber,questionType,correctAnswer</code></p>
      <div class="form-group"><label>Section ID</label><input class="form-input" id="q-section-id" type="number" placeholder="Section ID"></div>
      <div class="upload-zone" style="padding:30px;"
           ondragover="event.preventDefault(); this.classList.add('dragover')"
           ondragleave="this.classList.remove('dragover')"
           ondrop="handleQuestionDrop(event)"
           onclick="document.getElementById('q-file-input').click()">
        <input type="file" id="q-file-input" accept=".csv" onchange="handleQuestionFile(event)">
        <div class="icon">📤</div>
        <h3>Drop CSV file here</h3>
        <p class="text-muted">questionNumber,questionType,correctAnswer</p>
        <div id="q-file-info" class="file-info hidden"></div>
      </div>
      <button class="btn btn-primary mt-2" id="q-upload-btn" onclick="uploadQuestions()" disabled>Upload Questions</button>
    </div>

    <div id="question-tab-ingest" class="card mb-3 hidden">
      <h3 class="mb-2">Master PDF Ingest</h3>
      <p class="text-secondary mb-3" style="font-size:0.85rem;">Upload both the master Question Paper and official Answer Key PDF. We'll extract text, generate hashes, and seed everything automatically.</p>
      
      <div class="form-group"><label>Shift ID</label><input class="form-input" id="ingest-shift-id" type="number" placeholder="Enter Shift ID"></div>
      
      <div class="grid grid-2 gap-2 mt-2">
        <div class="form-group">
          <label>Question Paper PDF</label>
          <input type="file" id="ingest-qp-file" accept=".pdf" class="form-input">
        </div>
        <div class="form-group">
          <label>Answer Key PDF</label>
          <input type="file" id="ingest-ak-file" accept=".pdf" class="form-input">
        </div>
      </div>
      
      <button class="btn btn-primary mt-3 w-100" id="ingest-btn" onclick="ingestMasterPdfs()">🚀 Ingest & Seed Shift</button>
    </div>`;
}

function switchQuestionTab(tab) {
  document.getElementById('question-tab-bulk').classList.toggle('hidden', tab !== 'bulk');
  document.getElementById('question-tab-ingest').classList.toggle('hidden', tab !== 'ingest');
  document.querySelectorAll('.tab-btn').forEach(b => b.classList.toggle('active', b.textContent.toLowerCase().includes(tab)));
}

async function ingestMasterPdfs() {
  const shiftId = +document.getElementById('ingest-shift-id').value;
  const qpFile = document.getElementById('ingest-qp-file').files[0];
  const akFile = document.getElementById('ingest-ak-file').files[0];

  if (!shiftId || !qpFile || !akFile) {
    toast('Shift ID and both PDF files are required', 'error');
    return;
  }

  const btn = document.getElementById('ingest-btn');
  btn.disabled = true;
  btn.textContent = 'Ingesting...';

  try {
    const formData = new FormData();
    formData.append('questionPaper', qpFile);
    formData.append('answerKey', akFile);

    await api('POST', `/api/shifts/${shiftId}/ingest`, formData, true);
    toast('Shift seeded successfully from PDFs!', 'success');
  } catch (e) {
    toast('Ingest failed: ' + e.message, 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = '🚀 Ingest & Seed Shift';
  }
}

let questionFile = null;
function handleQuestionDrop(e) {
  e.preventDefault(); e.currentTarget.classList.remove('dragover');
  questionFile = e.dataTransfer.files[0]; showQuestionFileInfo();
}
function handleQuestionFile(e) { questionFile = e.target.files[0]; showQuestionFileInfo(); }
function showQuestionFileInfo() {
  if (!questionFile) return;
  document.getElementById('q-file-info').textContent = `✓ ${questionFile.name}`;
  document.getElementById('q-file-info').classList.remove('hidden');
  document.getElementById('q-upload-btn').disabled = false;
}

async function uploadQuestions() {
  const sectionId = +document.getElementById('q-section-id').value;
  if (!sectionId) { toast('Section ID is required', 'error'); return; }
  if (!questionFile) { toast('Please select a CSV file', 'error'); return; }

  const text = await questionFile.text();
  const lines = text.trim().split('\n');
  const questions = [];

  for (const line of lines) {
    const parts = line.split(',').map(p => p.trim());
    if (parts.length < 2 || isNaN(+parts[0])) continue;
    questions.push({
      questionNumber: +parts[0],
      questionType: parts.length >= 3 ? parts[1] : 'MCQ',
      correctAnswer: parts.length >= 3 ? parts[2] : parts[1]
    });
  }

  if (questions.length === 0) { toast('No valid entries in CSV', 'error'); return; }

  try {
    await api('POST', '/api/questions/bulk', { sectionId, questions });
    toast(`${questions.length} questions uploaded!`, 'success');
    questionFile = null;
    document.getElementById('q-file-info').classList.add('hidden');
    document.getElementById('q-upload-btn').disabled = true;
  } catch (e) { toast('Upload failed: ' + e.message, 'error'); }
}

// ANSWER KEYS
function renderAdminAnswerKeys() {
  return `
    <h2 class="mb-3">Answer Keys</h2>
    <p class="text-secondary mb-3">Answer keys are uploaded together with questions in the Questions section. Each question row includes the correct answer.</p>
    <button class="btn btn-secondary" onclick="adminNav('questions')">← Go to Questions</button>`;
}

// MARKING
function renderAdminMarking() {
  return `
    <div class="flex justify-between items-center mb-3">
      <h2>Marking Schemes</h2>
      <button class="btn btn-primary" onclick="showCreateMarkingModal()">+ Create Policy</button>
    </div>
    <div class="card mb-3">
      <h3 class="mb-2">View Policies by Exam Year</h3>
      <div class="form-group"><label>Exam Year ID</label>
        <div class="flex gap-1">
          <input class="form-input" id="marking-ey-id" type="number" placeholder="Exam Year ID">
          <button class="btn btn-secondary" onclick="loadMarkingPolicies()">Load</button>
        </div>
      </div>
      <div id="marking-list"></div>
    </div>`;
}

async function loadMarkingPolicies() {
  const eyId = +document.getElementById('marking-ey-id').value;
  if (!eyId) { toast('Enter Exam Year ID', 'error'); return; }
  try {
    const policies = await api('GET', `/api/marking-policies/by-exam-year/${eyId}`);
    document.getElementById('marking-list').innerHTML = policies.length === 0 ? '<p class="text-muted">No policies found</p>' :
      '<table class="results-table"><thead><tr><th>Scope</th><th>Correct</th><th>Negative</th><th>Unattempted</th><th></th></tr></thead><tbody>' +
      policies.map(p => `<tr><td>${p.sectionName}</td><td style="color:var(--success)">+${p.correctMarks}</td><td style="color:var(--danger)">-${p.negativeMarks}</td><td>${p.unattemptedMarks}</td><td><button class="btn btn-danger btn-sm" onclick="deletePolicy(${p.id})">×</button></td></tr>`).join('') +
      '</tbody></table>';
  } catch (e) { toast('Error: ' + e.message, 'error'); }
}

function showCreateMarkingModal() {
  showModal('Create Marking Policy', `
    <div class="form-group"><label>Exam Year ID</label><input class="form-input" id="modal-mp-eyid" type="number" placeholder="Exam Year ID"></div>
    <div class="form-group"><label>Section ID (leave empty for global)</label><input class="form-input" id="modal-mp-sid" type="number" placeholder="Optional"></div>
    <div class="form-group"><label>Correct Marks</label><input class="form-input" id="modal-mp-correct" type="number" step="0.25" value="2"></div>
    <div class="form-group"><label>Negative Marks</label><input class="form-input" id="modal-mp-negative" type="number" step="0.25" value="0.5"></div>
    <div class="form-group"><label>Unattempted Marks</label><input class="form-input" id="modal-mp-unattempted" type="number" step="0.25" value="0"></div>
  `, async () => {
    const eyId = +document.getElementById('modal-mp-eyid').value;
    const sId = document.getElementById('modal-mp-sid').value ? +document.getElementById('modal-mp-sid').value : null;
    const correct = +document.getElementById('modal-mp-correct').value;
    const negative = +document.getElementById('modal-mp-negative').value;
    const unattempted = +document.getElementById('modal-mp-unattempted').value;
    if (!eyId) { toast('Exam Year ID required', 'error'); return; }
    await api('POST', '/api/marking-policies', { examYearId: eyId, sectionId: sId, correctMarks: correct, negativeMarks: negative, unattemptedMarks: unattempted });
    toast('Marking policy created!', 'success');
    closeModal();
  });
}

async function deletePolicy(id) {
  try { await api('DELETE', `/api/marking-policies/${id}`); toast('Policy deleted', 'success'); loadMarkingPolicies(); }
  catch (e) { toast('Error: ' + e.message, 'error'); }
}

// CANDIDATES
function renderAdminCandidates() {
  return `
    <h2 class="mb-3">Candidate Count</h2>
    <div class="card">
      <p class="text-secondary mb-2">Update the total number of expected candidates for rank prediction.</p>
      <div class="form-group"><label>Exam Year ID</label><input class="form-input" id="cand-ey-id" type="number" placeholder="Exam Year ID"></div>
      <div class="form-group"><label>Total Candidates</label><input class="form-input" id="cand-total" type="number" placeholder="e.g., 2500000"></div>
      <button class="btn btn-primary" onclick="updateCandidates()">Update</button>
    </div>`;
}

async function updateCandidates() {
  const eyId = +document.getElementById('cand-ey-id').value;
  const total = +document.getElementById('cand-total').value;
  if (!eyId || !total) { toast('Both fields required', 'error'); return; }
  try {
    await api('PATCH', `/api/exam-years/${eyId}/candidates?totalCandidates=${total}`);
    toast('Candidate count updated!', 'success');
  } catch (e) { toast('Error: ' + e.message, 'error'); }
}

// ── Modal System ───────────────────────────────────────────────────────────
let modalCallback = null;

function showModal(title, content, onSubmit) {
  modalCallback = onSubmit;
  const overlay = document.createElement('div');
  overlay.className = 'modal-overlay';
  overlay.id = 'modal-overlay';
  overlay.onclick = (e) => { if (e.target === overlay) closeModal(); };
  overlay.innerHTML = `
    <div class="modal fade-in">
      <h2>${title}</h2>
      ${content}
      <div class="modal-actions">
        <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
        <button class="btn btn-primary" onclick="submitModal()">Save</button>
      </div>
    </div>`;
  document.body.appendChild(overlay);
}

function closeModal() {
  const overlay = document.getElementById('modal-overlay');
  if (overlay) overlay.remove();
  modalCallback = null;
}

async function submitModal() {
  if (modalCallback) {
    try { await modalCallback(); }
    catch (e) { toast('Error: ' + e.message, 'error'); }
  }
}

// async load admin stages when that panel is opened
async function loadAdminStagesPanel() {
  if (state.adminExams.length === 0) await loadAdminExams();
  const el = document.getElementById('admin-stages-list');
  if (!el) return;
  let html = '';
  for (const exam of state.adminExams) {
    try {
      const stages = await api('GET', `/api/exam-stages/by-exam/${exam.id}`);
      if (stages.length > 0) {
        html += `<h3 class="mb-2 mt-3">${exam.name}</h3><div class="grid grid-2">`;
        for (const s of stages) {
          html += `<div class="card"><div class="flex justify-between items-center mb-1"><h3>${s.name}</h3><button class="btn btn-danger btn-sm" onclick="deleteStage(${s.id})">Delete</button></div><p class="text-muted" style="font-size:0.85rem;">${s.description || ''} • ${s.years?.length || 0} years</p></div>`;
        }
        html += '</div>';
      }
    } catch (e) { console.error(e); }
  }
  el.innerHTML = html || '<div class="empty-state"><div class="icon">📑</div><p>No stages yet.</p></div>';
}

// Hook: when stages panel loads
const origRenderAdminStages = renderAdminStages;
// Override to auto-load
window.addEventListener('load', () => { /* will be called on nav */ });

// Watch for stages panel
const origAdminNav = adminNav;
adminNav = function (section) {
  state.adminSection = section;
  const main = document.getElementById('admin-main');
  if (main) { main.innerHTML = renderAdminSection(); }
  if (section === 'stages') setTimeout(loadAdminStagesPanel, 100);
};

async function deleteStage(id) {
  if (!confirm('Delete this stage?')) return;
  try { await api('DELETE', `/api/exam-stages/${id}`); toast('Stage deleted', 'success'); state.adminExams = []; state.exams = []; adminNav('stages'); }
  catch (e) { toast('Error: ' + e.message, 'error'); }
}

async function loadAdminSections() {
  // implement if needed
}

// ── Init ───────────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  updateRoleIndicator();
  updateNavigation();
  const path = window.location.pathname.substring(1);
  const validPages = ['admin', 'upload', 'result', 'stages', 'years', 'evaluator', 'student'];
  if (validPages.includes(path)) {
    navigate(path);
  } else {
    navigate('home');
  }
});

window.addEventListener('popstate', (e) => {
  if (e.state && e.state.page) {
    state.page = e.state.page;
    render();
  }
});
