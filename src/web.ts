import { WebPlugin } from '@capacitor/core';

import type { SendIntentPlugin } from './definitions';

export class SendIntentWeb extends WebPlugin implements SendIntentPlugin {
  constructor() {
    super();
  }

  async checkSendIntentReceived(): Promise<{ title: string }> {
    return { title: '' };
  }

  finish(): void {
    // no-op
  }
}
