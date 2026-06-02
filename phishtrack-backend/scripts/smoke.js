const base = 'http://127.0.0.1:3000';

async function showRes(res) {
  const ct = res.headers.get('content-type') || '';
  try {
    if (ct.includes('application/json')) {
      const j = await res.json();
      console.log(JSON.stringify(j, null, 2));
    } else {
      const t = await res.text();
      console.log(t);
    }
  } catch (e) {
    console.log('Failed to parse response', e.message);
  }
}

(async () => {
  try {
    console.log('\n== HEALTH ==');
    let res = await fetch(base + '/api/health');
    await showRes(res);

    console.log('\n== REGISTER ==');
    res = await fetch(base + '/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: 'Priyansh', email: 'test@example.com', password: 'Test@1234', organization: 'PhishTrack Labs' })
    });
    await showRes(res);

    console.log('\n== LOGIN ==');
    res = await fetch(base + '/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: 'test@example.com', password: 'Test@1234' })
    });
    const loginJson = await res.json().catch(() => null);
    console.log(JSON.stringify(loginJson, null, 2));

    let token = loginJson && loginJson.token ? loginJson.token : null;

    console.log('\n== CREATE CASE ==');
    res = await fetch(base + '/api/cases', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: JSON.stringify({ title: 'Suspicious phishing site', description: 'Phishing site mimicking bank login', reporterEmail: 'test@example.com' })
    });
    await showRes(res);

    console.log('\n== LIST CASES ==');
    res = await fetch(base + '/api/cases');
    await showRes(res);

  } catch (err) {
    console.error('Smoke test failed:', err);
    process.exitCode = 2;
  }
})();
