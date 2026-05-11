import { Component, OnInit } from '@angular/core';
import { JhiAlertService, JhiEventManager } from 'ng-jhipster';

import { JdlMetadataService } from './jdl-metadata.service';
import { JdlAiModelOption, JdlAiService } from './jdl-ai.service';

@Component({
  selector: 'jhi-jdl-ai-assistant',
  templateUrl: './jdl-ai-assistant.component.html'
})
export class JdlAiAssistantComponent implements OnInit {
  /** Ready-made prompts to exercise the assistant quickly (English works well with most models). */
  readonly samplePrompts: { label: string; text: string }[] = [
    {
      label: 'Library',
      text:
        'JHipster monolith, JWT auth, MySQL, Angular. Entities: Author (name, birthDate), Book (title, isbn, publicationDate), ' +
        'many-to-many Author–Book. Loan entity: user login, book, borrowedAt, returnedAt. Pagination and DTOs for Book.'
    },
    {
      label: 'E-commerce',
      text:
        'JHipster monolith, OAuth2 with Keycloak, PostgreSQL, React. Product (name, price, stock), Category, Order with OrderLine ' +
        '(quantity, unitPrice). Enums OrderStatus: PENDING, PAID, SHIPPED. Relationships and validation.'
    },
    {
      label: 'Blog',
      text:
        'JHipster monolith, JWT, H2 dev / PostgreSQL prod, Vue. Blog with title, slug, publishedAt. Post belongs to Blog; Comment on Post with authorName and body. User admin role for publishing.'
    },
    {
      label: 'Clinic',
      text:
        'JHipster monolith, JWT, MariaDB. Patient (firstName, lastName, birthDate), Doctor (specialty enum: GP, CARDIOLOGY, PEDIATRICS), ' +
        'Appointment linking Patient and Doctor with start, end, notes. Service layer and REST for appointments only for authenticated users.'
    },
    {
      label: 'IoT minimal',
      text:
        'Small JHipster monolith, JWT, MongoDB. Device (deviceId unique, firmwareVersion, lastSeen Instant), SensorReading (device, metric, value double, timestamp). Index-friendly fields.'
    }
  ];

  assistantEnabled = false;
  ragEnabled = false;
  helpText = '';
  models: JdlAiModelOption[] = [];
  selectedModelId = '';
  prompt = '';
  generatedJdl = '';
  loading = false;
  openingStudio = false;

  constructor(
    private jdlAiService: JdlAiService,
    private jdlMetadataService: JdlMetadataService,
    private alertService: JhiAlertService,
    private eventManager: JhiEventManager
  ) {}

  ngOnInit(): void {
    this.jdlAiService.getConfig().subscribe(
      c => {
        this.assistantEnabled = c.enabled;
        this.ragEnabled = !!c.ragEnabled;
        this.helpText = c.helpText || '';
        this.models = c.models ?? [];
        const def = (c.defaultModelId || '').trim();
        if (def && this.models.some(m => m.id === def)) {
          this.selectedModelId = def;
        } else if (this.models.length > 0) {
          this.selectedModelId = this.models[0].id;
        } else {
          this.selectedModelId = '';
        }
      },
      () => {
        this.assistantEnabled = false;
        this.models = [];
        this.selectedModelId = '';
      }
    );
  }

  useSamplePrompt(entry: { label: string; text: string }): void {
    this.prompt = entry.text;
  }

  canGenerate(): boolean {
    return !!(this.prompt && this.prompt.trim().length > 0);
  }

  generate(): void {
    if (!this.canGenerate()) {
      return;
    }
    this.loading = true;
    this.generatedJdl = '';
    this.jdlAiService.generate(this.prompt.trim(), this.selectedModelId).subscribe(
      res => {
        this.generatedJdl = res.jdl;
        this.loading = false;
      },
      err => {
        const body = err.error as { detail?: string; message?: string } | undefined;
        const fromServer = body?.detail || body?.message;
        const fallback =
          'Could not generate JDL. On Developer Sandbox: set APPLICATION_JDL_AI_ENABLED=true, APPLICATION_JDL_AI_API_KEY ' +
          '(e.g. oc whoami -t), APPLICATION_JDL_AI_INSECURE_TLS=true, and ensure the pod can reach sandbox-shared-models inference URLs.';
        this.alertService.error(fromServer || fallback, null);
        this.loading = false;
      }
    );
  }

  copy(): void {
    if (!this.generatedJdl) {
      return;
    }
    navigator.clipboard.writeText(this.generatedJdl).then(
      () => this.alertService.success('Copied to clipboard', null),
      () => this.alertService.error('Copy failed (clipboard may be unavailable)', null)
    );
  }

  /**
   * JDL Studio loads models by id from the server. Persist the AI draft, then navigate to the bundled studio.
   */
  openDraftInJdlStudio(): void {
    if (!this.generatedJdl?.trim()) {
      return;
    }
    this.openingStudio = true;
    const name = `AI draft ${new Date()
      .toISOString()
      .slice(0, 19)
      .replace('T', ' ')}`;
    this.jdlMetadataService.createJdl(name, this.generatedJdl).subscribe({
      next: meta => {
        this.openingStudio = false;
        if (meta?.id) {
          this.eventManager.broadcast('jdlMetadataListModification');
          window.location.href = `jdl-studio/#!/view/${meta.id}`;
        } else {
          this.alertService.error('Could not open JDL Studio (missing model id).', null);
        }
      },
      error: () => {
        this.openingStudio = false;
        this.alertService.error('Could not save the draft to open in JDL Studio. Try Copy JDL and paste manually.', null);
      }
    });
  }
}
