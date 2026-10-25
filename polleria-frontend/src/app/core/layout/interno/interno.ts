import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { AppNavigation } from '../../../shared/navigation/app-navigation';

@Component({
  selector: 'app-layout-interno',
  imports: [RouterLink, RouterOutlet, AppNavigation],
  templateUrl: './interno.html',
  styleUrl: './interno.css',
})
export class Interno {}
