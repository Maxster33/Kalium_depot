// Client de l'API de KG_ScoreBoards (cahier des charges, section 2). Le bot interroge, le serveur repond.
// Chaque reponse reussie est gardee en memoire : si kal-games est eteint, on rend la derniere reponse connue en le
// signalant (stale = true), au lieu d'une erreur.

export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

export class ScoreApi {
  constructor(baseUrl, token) {
    this.baseUrl = baseUrl.replace(/\/+$/, '');
    this.token = token;
    this.cache = new Map();
  }

  /** GET sur l'API ; renvoie { data, at, stale }. 404 -> null (pas de repli sur le cache). */
  async get(path, { cache = true } = {}) {
    try {
      const response = await fetch(`${this.baseUrl}/api/v1/${path}`, {
        headers: { Authorization: `Bearer ${this.token}` },
        signal: AbortSignal.timeout(8000),
      });
      if (response.status === 404) return null;
      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new ApiError(body.error ?? `HTTP ${response.status}`, response.status);
      }
      const result = { data: await response.json(), at: new Date(), stale: false };
      if (cache) this.cache.set(path, result);
      return result;
    } catch (error) {
      // Jeton refuse : erreur de configuration, jamais masquee par le cache.
      if (error instanceof ApiError && error.status === 401) throw error;
      const cached = cache ? this.cache.get(path) : undefined;
      if (cached) return { ...cached, stale: true };
      throw error instanceof ApiError ? error : new ApiError('serveur de jeu injoignable', 0);
    }
  }

  status() {
    return this.get('status');
  }

  ranking(game, { month = false, lap = false, limit = 10 } = {}) {
    const query = new URLSearchParams({ period: month ? 'month' : 'general', sort: lap ? 'lap' : 'points', limit: String(limit) });
    return this.get(`rankings/${encodeURIComponent(game)}?${query}`);
  }

  archives() {
    return this.get('archives');
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
