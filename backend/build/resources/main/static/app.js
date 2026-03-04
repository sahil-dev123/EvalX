/* ==========================================================================
   EvalX — SPA Application
   ========================================================================== */

const API = '';

// ── State ──────────────────────────────────────────────────────────────────
const state = {
  page: 'home',
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
  adminSection: 'dashboard',
  adminToken: localStorage.getItem('evalx_admin_token') || null,
  adminExams: [],
  adminStages: [],
  adminYears: [],
  adminSections: [],
  adminQuestions: [],
  adminPolicies: [],
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
}

// ── Router ─────────────────────────────────────────────────────────────────
function navigate(page, data) {
  state.page = page;
  if (data) Object.assign(state, data);
  render();
  window.scrollTo({ top: 0, behavior: 'smooth' });
  // Update nav
  document.querySelectorAll('.nav-links button').forEach(b => b.classList.remove('active'));
  const navEl = document.getElementById(`nav-${page === 'admin' ? 'admin' : 'home'}`);
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
    const res = await fetch(`${API}/api/evaluate?examYearId=${state.selectedYear.id}`, {
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
          <p class="text-secondary">${state.selectedExam?.name} — ${state.selectedStage?.name} — ${state.selectedYear?.year}</p>
          <div class="score-display text-gradient">${r.totalScore} / ${r.maxScore}</div>
          <p class="score-sub">Your Score</p>
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
    toast('Login successful!', 'success');
    navigate('admin');
  } catch (e) { toast(e.message, 'error'); }
}

function adminLogout() {
  state.adminToken = null;
  localStorage.removeItem('evalx_admin_token');
  toast('Logged out', 'success');
  navigate('admin');
}

function renderAdmin() {
  if (!state.adminToken) return renderAdminLogin();
  return `
    <div class="admin-layout">
      <aside class="admin-sidebar">
        <div class="menu-label">Management</div>
        ${['dashboard', 'exams', 'stages', 'sections', 'questions', 'answer-keys', 'marking', 'candidates'].map(s =>
    `<button class="menu-item ${state.adminSection === s ? 'active' : ''}" onclick="adminNav('${s}')">
            <span>${{ 'dashboard': '📊', 'exams': '📋', 'stages': '📑', 'sections': '📁', 'questions': '❓', 'answer-keys': '🔑', 'marking': '⚖️', 'candidates': '👥' }[s]}</span>
            ${s.split('-').map(w => w[0].toUpperCase() + w.slice(1)).join(' ')}
          </button>`
  ).join('')}
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
    case 'dashboard': return renderAdminDashboard();
    case 'exams': return renderAdminExams();
    case 'stages': return renderAdminStages();
    case 'sections': return renderAdminSections();
    case 'questions': return renderAdminQuestions();
    case 'answer-keys': return renderAdminAnswerKeys();
    case 'marking': return renderAdminMarking();
    case 'candidates': return renderAdminCandidates();
    default: return renderAdminDashboard();
  }
}

function renderAdminDashboard() {
  loadAdminExams();
  return `
    <h2 class="mb-3">Admin Dashboard</h2>
    <div class="grid grid-4 mb-4">
      <div class="card stat-card accent"><div class="stat-value" id="admin-exam-count">—</div><div class="stat-label">Exams</div></div>
      <div class="card stat-card info"><div class="stat-value" id="admin-stage-count">—</div><div class="stat-label">Stages</div></div>
      <div class="card stat-card success"><div class="stat-value" id="admin-section-count">—</div><div class="stat-label">Sections</div></div>
      <div class="card stat-card warning"><div class="stat-value" id="admin-year-count">—</div><div class="stat-label">Exam Years</div></div>
    </div>
    <div class="card">
      <h3 class="mb-2">Quick Start Guide</h3>
      <table class="results-table">
        <tr><td>1.</td><td>Create an <strong>Exam</strong> (e.g., SSC CGL, GATE)</td><td><button class="btn btn-sm btn-secondary" onclick="adminNav('exams')">Go →</button></td></tr>
        <tr><td>2.</td><td>Add <strong>Stages</strong> (e.g., Pre, Mains)</td><td><button class="btn btn-sm btn-secondary" onclick="adminNav('stages')">Go →</button></td></tr>
        <tr><td>3.</td><td>Create <strong>Exam Year</strong> with candidate count</td><td><button class="btn btn-sm btn-secondary" onclick="adminNav('stages')">Go →</button></td></tr>
        <tr><td>4.</td><td>Add <strong>Sections</strong> (e.g., Reasoning, GA)</td><td><button class="btn btn-sm btn-secondary" onclick="adminNav('sections')">Go →</button></td></tr>
        <tr><td>5.</td><td>Upload <strong>Questions & Answer Keys</strong> via CSV</td><td><button class="btn btn-sm btn-secondary" onclick="adminNav('questions')">Go →</button></td></tr>
        <tr><td>6.</td><td>Set <strong>Marking Scheme</strong> (+2, −0.5)</td><td><button class="btn btn-sm btn-secondary" onclick="adminNav('marking')">Go →</button></td></tr>
      </table>
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
    <h2 class="mb-3">Upload Questions & Answer Keys</h2>
    <div class="card mb-3">
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
    </div>`;
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
  navigate('home');
});
