/**
 * JHipster 8 worker: runs legacy blueprints in an isolated Node image and returns a tar.gz of the generated tree.
 */
const express = require('express');
const { execFileSync } = require('child_process');
const fs = require('fs');
const os = require('os');
const path = require('path');

const PORT = Number(process.env.PORT || 8081);

const app = express();
app.use(express.text({ type: ['text/plain', 'application/json'], limit: '8mb' }));

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

app.get('/health', (_req, res) => {
  res.status(200).type('text/plain').send('ok');
});

app.post('/generate', (req, res) => {
  const yoRcText = typeof req.body === 'string' ? req.body : JSON.stringify(req.body);
  let workDir;
  try {
    const cli = resolveCli(yoRcText);
    workDir = fs.mkdtempSync(path.join(os.tmpdir(), 'jh8w-'));
    fs.writeFileSync(path.join(workDir, '.yo-rc.json'), yoRcText, 'utf8');

    const args = ['--force-insight', '--skip-checks', '--skip-install', '--skip-cache', '--skip-git', '--force'];
    try {
      execFileSync(cli, args, {
        cwd: workDir,
        encoding: 'utf8',
        stdio: ['ignore', 'pipe', 'pipe'],
        maxBuffer: 50 * 1024 * 1024,
        env: { ...process.env, CI: 'true' }
      });
    } catch (genErr) {
      const out = genErr.stdout ? String(genErr.stdout) : '';
      const errOut = genErr.stderr ? String(genErr.stderr) : '';
      throw new Error([out, errOut, genErr.message || String(genErr)].filter(Boolean).join('\n'));
    }
  } catch (e) {
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
  } catch (e) {
    res.status(500).type('text/plain').send('tar failed: ' + (e.message || String(e)));
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
