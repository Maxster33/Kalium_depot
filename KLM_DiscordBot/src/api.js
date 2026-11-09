// Client de l'API de KG_ScoreBoards (cahier des charges, section 2). Le bot interroge, le serveur repond.
// Chaque reponse reussie est gardee en memoire : si le serveur de jeu est eteint, on rend la derniere reponse connue en
// le signalant (stale = true), au lieu d'une erreur.
//
// 0.3.0 : deux chemins. Par le relais (RELAIS_URL / RELAIS_TOKEN, KaliumRelay 1.11.0) : le bot depose sa demande dans
// la boite « scoreboards » du relais, KG_ScoreBoards 1.10.0 (Kanvas) la releve chaque seconde et depose la reponse dans
// la boite « bot ». En direct (API_URL / API_TOKEN) : l'API HTTP de KG_ScoreBoards, si son port est joignable.

export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

/** API HTTP de KG_ScoreBoards en direct. */
export class HttpTransport {
  constructor(baseUrl, token) {
    this.baseUrl = baseUrl.replace(/\/+$/, '');
    this.token = token;
  }

  async request(path) {
    const response = await fetch(`${this.baseUrl}/api/v1/${path}`, {
      headers: { Authorization: `Bearer ${this.token}` },
      signal: AbortSignal.timeout(8000),
    });
    return { status: response.status, body: await response.text() };
  }
}

/** Boite aux lettres de KaliumRelay : demande deposee, reponse relevee (voir RelaisBot dans KG_ScoreBoards). */
export class RelaisTransport {
  constructor(url, token, { delai = 10000, intervalle = 400 } = {}) {
    this.url = url.replace(/\/+$/, '');
    this.token = token;
    this.delai = delai;
    this.intervalle = intervalle;
    this.attente = new Map(); // id de la demande -> { resolve, timer }
    this.releve = null;
  }

  async request(path) {
    const response = await this.relais('POST', '/mail/scoreboards', `/api/v1/${path}`);
    if (response.status === 401 || response.status === 403) throw new ApiError('jeton du relais refusé', 401);
    if (response.status !== 200) throw new ApiError(`relais : HTTP ${response.status}`, 0);
    const id = (await response.text()).trim();
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        this.attente.delete(id);
        // Demande abandonnee : retiree de la boite, pour que le serveur ne la traite pas plus tard pour rien.
        this.relais('DELETE', `/mail/scoreboards/${encodeURIComponent(id)}`).catch(() => {});
        reject(new ApiError('serveur de jeu injoignable', 0));
      }, this.delai);
      this.attente.set(id, { resolve, timer });
      this.planifier();
    });
  }

  planifier() {
    if (!this.releve && this.attente.size) this.releve = setTimeout(() => this.relever(), this.intervalle);
  }

  async relever() {
    try {
      const response = await this.relais('GET', '/mail/bot');
      if (response.ok) {
        for (const ligne of (await response.text()).split('\n')) {
          const tab = ligne.indexOf('\t');
          if (tab <= 0) continue;
          const message = ligne.slice(0, tab);
          const texte = Buffer.from(ligne.slice(tab + 1).trim(), 'base64').toString('utf8');
          const [id, code] = texte.split('\n', 2);
          const body = texte.slice(id.length + code.length + 2);
          // Toujours confirme, meme une reponse arrivee trop tard : la boite ne doit pas grossir.
          this.relais('DELETE', `/mail/bot/${encodeURIComponent(message)}`).catch(() => {});
          const attendu = this.attente.get(id);
          if (attendu) {
            this.attente.delete(id);
            clearTimeout(attendu.timer);
            attendu.resolve({ status: Number(code), body });
          }
        }
      }
    } catch {
      // relais injoignable : les demandes en attente finiront par expirer
    } finally {
      this.releve = null;
      this.planifier();
    }
  }

  relais(method, path, body) {
    return fetch(`${this.url}${path}`, {
      method,
      headers: { 'X-Kalium-Relay-Token': this.token },
      body,
      signal: AbortSignal.timeout(8000),
    });
  }
}

export class ScoreApi {
  constructor(transport) {
    this.transport = transport;
    this.cache = new Map();
  }

  /**
   * GET sur l'API ; renvoie { data, at, stale }. 404 -> null (pas de repli sur le cache). maxAge : reponse en cache
   * plus recente que maxAge ms rendue sans redemander (autocompletion : Discord n'attend que 3 s).
   */
  async get(path, { cache = true, maxAge = 0 } = {}) {
    const cached = cache ? this.cache.get(path) : undefined;
    if (cached && maxAge && Date.now() - cached.at.getTime() < maxAge) return cached;
    try {
      const response = await this.transport.request(path);
      if (response.status === 404) return null;
      if (response.status < 200 || response.status >= 300) {
        let message = `HTTP ${response.status}`;
        try {
          message = JSON.parse(response.body).error ?? message;
        } catch {
          // corps illisible : code seul
        }
        throw new ApiError(message, response.status);
      }
      const result = { data: JSON.parse(response.body), at: new Date(), stale: false };
      if (cache) this.cache.set(path, result);
      return result;
    } catch (error) {
      // Jeton refuse : erreur de configuration, jamais masquee par le cache.
      if (error instanceof ApiError && error.status === 401) throw error;
      if (cached) return { ...cached, stale: true };
      throw error instanceof ApiError ? error : new ApiError('serveur de jeu injoignable', 0);
    }
  }

  status({ maxAge = 0 } = {}) {
    return this.get('status', { maxAge });
  }

  ranking(game, { month = false, lap = false, limit = 10 } = {}) {
    const query = new URLSearchParams({ period: month ? 'month' : 'general', sort: lap ? 'lap' : 'points', limit: String(limit) });
    return this.get(`rankings/${encodeURIComponent(game)}?${query}`);
  }

  archives({ maxAge = 0 } = {}) {
    return this.get('archives', { maxAge });
  }

  archive(id, game, { lap = false, limit = 10 } = {}) {
    const query = new URLSearchParams({ sort: lap ? 'lap' : 'points', limit: String(limit) });
    return this.get(`archives/${encodeURIComponent(id)}/${encodeURIComponent(game)}?${query}`);
  }

  player(who) {
    return this.get(`players/${encodeURIComponent(who)}`);
  }

  events(after, limit = 500) {
    const query = new URLSearchParams({ limit: String(limit) });
    if (after) query.set('after', after);
    // Jamais de cache : un evenement deja rendu ne doit pas revenir (annonces en double).
    return this.get(`events?${query}`, { cache: false });
  }
}
