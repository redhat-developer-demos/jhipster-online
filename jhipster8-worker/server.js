/**
 * JHipster 8 worker: runs legacy blueprints in an isolated Node image and returns a tar.gz of the generated tree.
 */
const express = require('express');
const { spawnSync } = require('child_process');
const fs = require('fs');
const os = require('os');
const path = require('path');

const PORT = Number(process.env.PORT || 8081);

function logLine(msg) {
  // eslint-disable-next-line no-console
  console.log(`[jh8-worker ${new Date().toISOString()}] ${msg}`);
}

const app = express();
app.use(express.text({ type: ['text/plain', 'application/json'], limit: '8mb' }));

/**
 * JHipster Online sends databaseType "sql" (JH9); JH8 blueprints expect a concrete engine (mariadb, postgresql, …).
 */
function shimYoRcForJh8(yoRcText) {
  const obj = JSON.parse(yoRcText);
  const gen = obj['generator-jhipster'];
  if (gen && gen.databaseType === 'sql' && gen.prodDatabaseType) {
    gen.databaseType = gen.prodDatabaseType;
    logLine('Shim: databaseType sql -> ' + gen.prodDatabaseType);
  }
  return JSON.stringify(obj, null, 2);
}

function resolveCli(yoRcText) {
  const s = typeof yoRcText === 'string' ? yoRcText : JSON.stringify(yoRcText);
  if (s.includes('generator-jhipster-dotnetcore')) {
    return 'jhipster-dotnetcore';
  }
  if (s.includes('generator-jhipster-azure-container-apps')) {
    return 'jhipster-azure-container-apps';
  }
  if (s.includes('generator-jhipster-nodejs') || s.includes('generator-jhipster-nestjs')) {
    return 'nhipster';
  }
  if (s.includes('"backendFramework":"dotnet"')) {
    return 'jhipster-dotnetcore';
  }
  if (s.includes('"backendFramework":"azure-aca"')) {
    return 'jhipster-azure-container-apps';
  }
  if (s.includes('"backendFramework":"node"')) {
    return 'nhipster';
  }
  throw new Error(
    'Unrecognized JHipster 8 stack: expected blueprints generator-jhipster-dotnetcore, generator-jhipster-nodejs/nestjs, or generator-jhipster-azure-container-apps.'
  );
}

/** Not all JH8 CLIs accept the same flags as jhipster-dotnetcore. */
function resolveArgs(cli) {
  const base = ['--force-insight', '--skip-checks', '--skip-install', '--skip-cache', '--force'];
  if (cli === 'jhipster-azure-container-apps' || cli === 'nhipster') {
    return base;
  }
  return [...base, '--skip-git'];
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

app.get('/health', (_req, res) => {
  res.status(200).type('text/plain').send('ok');
});

app.post('/generate', (req, res) => {
  const rawBody = typeof req.body === 'string' ? req.body : JSON.stringify(req.body);
  let workDir;
  const t0 = Date.now();
  try {
    const cli = resolveCli(rawBody);
    logLine('POST /generate cli=' + cli);
    let yoRcText;
    try {
      yoRcText = shimYoRcForJh8(rawBody);
    } catch (e) {
      logLine('WARN: yo-rc shim parse failed, using raw body: ' + e.message);
      yoRcText = rawBody;
    }
    workDir = fs.mkdtempSync(path.join(os.tmpdir(), 'jh8w-'));
    fs.writeFileSync(path.join(workDir, '.yo-rc.json'), yoRcText, 'utf8');

    const args = resolveArgs(cli);
    logLine('spawn: ' + cli + ' ' + args.join(' '));
    const result = spawnSync(cli, args, {
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
    const { execFileSync } = require('child_process');
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
  console.log('jhipster8-worker listening on ' + PORT);
});
