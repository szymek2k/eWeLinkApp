import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { WattService } from './watt.service';
import { HttpClient } from '@angular/common/http';

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

  constructor(private wattService: WattService) {
    wattService.getInitMessage().subscribe((v) => (this.initMessage = v));
  }
}
