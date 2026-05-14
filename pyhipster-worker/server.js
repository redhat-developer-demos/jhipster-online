/**
 * PyHipster worker: runs generator-pyhipster (Yeoman 5) in an isolated Node 18 image and returns a tar.gz of the generated tree.
 */
const express = require('express');
const { spawnSync, execFileSync } = require('child_process');
const fs = require('fs');
const os = require('os');
const path = require('path');

const PORT = Number(process.env.PORT || 8082);

function logLine(msg) {
  // eslint-disable-next-line no-console
  console.log(`[pyhipster-worker ${new Date().toISOString()}] ${msg}`);
}

function logChunk(prefix, data) {
  if (data == null || data === '') {
    return;
  }
  String(data)
    .split(/\r?\n/)
    .forEach(line => {
      if (line.length > 0) {
        logLine(prefix + ' ' + line);
      }
    });
}

/**
 * JHipster Online nests config under `generator-jhipster`; PyHipster reads `generator-pyhipster`.
 * Also maps databaseType "sql" (JH9-style) to concrete prodDatabaseType when present.
 */
function shimYoRcForPyhipster(yoRcText) {
  const obj = JSON.parse(yoRcText);
  const jh = obj['generator-jhipster'];
  if (jh && !obj['generator-pyhipster']) {
    obj['generator-pyhipster'] = JSON.parse(JSON.stringify(jh));
    logLine('Shim: copied generator-jhipster -> generator-pyhipster');
  }
  const py = obj['generator-pyhipster'];
  if (py && py.databaseType === 'sql' && py.prodDatabaseType) {
    py.databaseType = py.prodDatabaseType;
    logLine('Shim: generator-pyhipster.databaseType sql -> ' + py.prodDatabaseType);
  }
  return JSON.stringify(obj, null, 2);
}

const app = express();
app.use(express.text({ type: ['text/plain', 'application/json'], limit: '8mb' }));

app.get('/health', (_req, res) => {
  res.status(200).type('text/plain').send('ok');
});

const PY_ARGS = ['--force-insight', '--skip-checks', '--skip-install', '--skip-cache', '--skip-git', '--force'];

app.post('/generate', (req, res) => {
  const rawBody = typeof req.body === 'string' ? req.body : JSON.stringify(req.body);
  let workDir;
  const t0 = Date.now();
  try {
    logLine('POST /generate');
    let yoRcText;
    try {
      yoRcText = shimYoRcForPyhipster(rawBody);
    } catch (e) {
      logLine('WARN: yo-rc shim failed, using raw body: ' + e.message);
      yoRcText = rawBody;
    }
    workDir = fs.mkdtempSync(path.join(os.tmpdir(), 'pyh-'));
    fs.writeFileSync(path.join(workDir, '.yo-rc.json'), yoRcText, 'utf8');

    logLine('spawn: pyhipster ' + PY_ARGS.join(' '));
    const result = spawnSync('pyhipster', PY_ARGS, {
      cwd: workDir,
      encoding: 'utf8',
      maxBuffer: 50 * 1024 * 1024,
      env: { ...process.env, CI: 'true' }
    });
    logChunk('[stdout]', result.stdout);
    logChunk('[stderr]', result.stderr);
    if (result.error) {
      throw result.error;
    }
    if (result.status !== 0) {
      throw new Error(
        'Command exited with code ' +
          result.status +
          (result.signal ? ' signal=' + result.signal : '') +
          '\n' +
          (result.stderr || '') +
          '\n' +
          (result.stdout || '')
      );
    }
    logLine('generation finished in ' + (Date.now() - t0) + 'ms');
  } catch (e) {
    logLine('ERROR: ' + (e.message || String(e)));
    if (workDir && fs.existsSync(workDir)) {
      try {
        fs.rmSync(workDir, { recursive: true, force: true });
      } catch (_) {
        /* ignore */
      }
    }
    return res.status(500).type('text/plain').send(e.message || String(e));
  }

  try {
    const tarball = execFileSync('tar', ['-czf', '-', '-C', workDir, '.'], {
      encoding: 'buffer',
      maxBuffer: 512 * 1024 * 1024
    });
    res.status(200).type('application/gzip').send(tarball);
    logLine('tar response sent in ' + (Date.now() - t0) + 'ms total');
  } catch (e) {
    return res.status(500).type('text/plain').send('tar failed: ' + (e.message || String(e)));
  } finally {
    if (workDir && fs.existsSync(workDir)) {
      try {
        fs.rmSync(workDir, { recursive: true, force: true });
      } catch (_) {
        /* ignore */
      }
    }
  }
});

app.listen(PORT, '0.0.0.0', () => {
  // eslint-disable-next-line no-console
  console.log('pyhipster-worker listening on ' + PORT);
});
