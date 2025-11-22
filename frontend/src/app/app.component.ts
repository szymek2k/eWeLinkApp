
import { RouterOutlet } from '@angular/router';
import { WattService } from './watt.service';
import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, signal, OnInit, effect } from '@angular/core';

// Konfiguracja URL Twojego serwera Spring Boot
// Używamy localhost, ponieważ tam działa Twój backend
const SPRING_BASE_URL = 'http://localhost:8080';
@Component({
  selector: 'app-root',
 // standalone: true,
  //imports: [RouterOutlet],
  providers: [WattService, HttpClient],
  templateUrl: './app.component.html',
  styleUrl: './app.component.sass',
})
export class AppComponent {
  title = 'ewelinkAppF';
  initMessage = '';

   // Stan aplikacji: 'start', 'callback'
  step = signal<'start' | 'callback'>('start');
  isLoading = signal(false);
  error = signal<string | null>(null);
  authResult = signal<string | null>(null);

  constructor(private wattService: WattService,private http: HttpClient) {
   // wattService.getInitMessage().subscribe((v) => (this.initMessage = v));
  }

    ngOnInit(): void {
    // Sprawdzenie, czy URL zawiera parametry zwrotne ('code' lub 'state')
    const urlParams = new URLSearchParams(window.location.search);
    const code = urlParams.get('code');
    
    // Jeśli w URL jest parametr 'code', oznacza to, że jesteśmy po przekierowaniu od eWeLink
    if (code) {
      this.step.set('callback');
      this.handleCallback();
    }
  }

  startLogin(): void {
    this.isLoading.set(true);
    this.error.set(null);

    // Endpoint /auth/login w Twojej aplikacji zwraca 302 z adresem eWeLink.
    // Używamy opcji { observe: 'response' } i 'responseType: 'text''
    // aby zapobiec automatycznemu parsowaniu odpowiedzi i obsłużyć przekierowanie.
    this.http.get(SPRING_BASE_URL + '/auth/login', { observe: 'response', responseType: 'text' })
      .subscribe({
        next: (response) => {
          // Zazwyczaj udane przekierowanie powinno być status 302, 
          // ale przeglądarka Angulara traktuje to jako udaną odpowiedź z bazy URL.
          // Sprawdzamy, czy w ciele odpowiedzi Spring nie odesłał prefiksu "redirect:".
          const redirectUrl = response.body;

          if (redirectUrl && redirectUrl.startsWith('redirect:')) {
             // W środowisku front-end (przeglądarki), musimy ręcznie wykonać przekierowanie
             // na adres eWeLink, który zwraca Spring.
             const ewelinkUrl = redirectUrl.substring('redirect:'.length);
             console.log('Otrzymano URL eWeLink. Przekierowuję:', ewelinkUrl);
             window.location.href = ewelinkUrl;

          } else if (response.status === 200 && response.url === SPRING_BASE_URL + '/auth/login') {
             // Jeśli Twój endpoint Springa jest poprawnie skonfigurowany,
             // powinien zwrócić 302, ale przeglądarki czasem "ukrywają" ten status.
             // W tym scenariuszu, po prostu przekierowujemy na eWeLink,
             // zakładając, że Spring prawidłowo zareagował.
             // Ze względu na ograniczenia CORS i 302 w XHR, jest to trudne do przetestowania.
             // Najlepszą strategią jest zaufanie, że Spring obsłuży 302.
             this.error.set('Błąd: Otrzymano nieoczekiwaną odpowiedź. Upewnij się, że Spring zwraca "redirect:URL"');
          } else {
             this.error.set('Nieoczekiwana odpowiedź serwera.');
          }
        },
        error: (err) => {
          // Błędy CORS/sieci
          this.isLoading.set(false);
          this.error.set('Błąd połączenia z backendem Spring: ' + (err.message || 'Brak odpowiedzi.'));
          // UWAGA: Jeśli Twoja metoda /auth/login działa, ale rzuca błąd CORS, 
          // to jest to dobry znak, że przekierowanie jest blokowane,
          // ale i tak możemy spróbować przekierować (jeśli znasz stały URL eWeLink).
        }
      });
  }

   private handleCallback(): void {
    this.isLoading.set(true);
    this.error.set(null);
    
    // Zrzucamy parametry z paska adresu (code i state)
    const callbackUrl = window.location.href.split(SPRING_BASE_URL)[1];
    
    // Uderzamy w endpoint /auth/return z kompletnymi parametrami CODE i STATE
    // Ten endpoint na backendzie uruchamia metodę authService.authenticateOrRefresh(code)
    this.http.get(SPRING_BASE_URL + callbackUrl, { responseType: 'text' })
        .subscribe({
            next: (response) => {
                // Odbieramy Mono<String> (HTML lub komunikat) od Spring Boot
                this.authResult.set(response);
                this.isLoading.set(false);
            },
            error: (err) => {
                this.isLoading.set(false);
                this.error.set('Błąd w trakcie wymiany tokenu (backend /auth/return): ' + (err.error || err.message));
            }
        });
  }

  resetApp(): void {
      window.location.href = SPRING_BASE_URL + '/auth/login';
  }
}
