import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { AppNavigation } from '../../../shared/navigation/app-navigation';

@Component({
  selector: 'app-layout-publico',
  imports: [RouterLink, RouterOutlet, AppNavigation],
  templateUrl: './publico.html',
  styleUrl: './publico.css',
})
export class Publico {}
