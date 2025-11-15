import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";

@Injectable({ providedIn: 'root' })
export class WattService {
    private backendUrl = 'http://localhost:8080';
  constructor(private http: HttpClient) {}

  getInitMessage(): Observable<string> {
    return this.http.get(this.backendUrl+'/initMessage', { responseType: 'text' });
  }
}
