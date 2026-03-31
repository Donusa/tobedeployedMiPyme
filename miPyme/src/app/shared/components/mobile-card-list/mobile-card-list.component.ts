import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { MobileCardColumn, MobileCardAction } from './mobile-card-list.models';

interface CardRow {
  item: any;
  expanded: boolean;
}

@Component({
  selector: 'app-mobile-card-list',
  templateUrl: './mobile-card-list.component.html',
  styleUrls: ['./mobile-card-list.component.css']
})
export class MobileCardListComponent implements OnChanges {


  @Input() items: any[] = [];


  @Input() columns: MobileCardColumn[] = [];


  @Input() actions: MobileCardAction[] = [];


  @Input() collapsedFieldCount = 3;


  @Input() emptyMessage = 'No hay registros para mostrar.';

  rows: CardRow[] = [];

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['items']) {
      this.rows = (this.items || []).map(item => ({ item, expanded: false }));
    }
  }

  toggleExpand(row: CardRow): void {
    row.expanded = !row.expanded;
  }

  getPrimaryColumn(): MobileCardColumn | undefined {
    return this.columns.find(c => c.isPrimary);
  }

  getSecondaryColumns(expanded: boolean): MobileCardColumn[] {
    const secondary = this.columns.filter(c => !c.isPrimary);
    if (expanded) return secondary;
    return secondary.filter(c => !c.expandOnly).slice(0, this.collapsedFieldCount);
  }

  hasExpandableContent(row: CardRow): boolean {
    const secondary = this.columns.filter(c => !c.isPrimary);
    const collapsed = secondary.filter(c => !c.expandOnly).slice(0, this.collapsedFieldCount);
    return secondary.length > collapsed.length || secondary.some(c => c.expandOnly);
  }

  getValue(item: any, col: MobileCardColumn): string {
    const raw = this.resolveField(item, col.field);
    if (col.format) return col.format(raw, item);
    if (raw === null || raw === undefined) return '—';
    return String(raw);
  }

  getBadgeClass(item: any, col: MobileCardColumn): string {
    if (!col.badgeClass) return '';
    if (typeof col.badgeClass === 'function') return col.badgeClass(item);
    return col.badgeClass;
  }

  getVisibleActions(item: any): MobileCardAction[] {
    return this.actions.filter(a => !a.condition || a.condition(item));
  }

  trackByIndex(index: number): number {
    return index;
  }

  private resolveField(obj: any, path: string): any {
    return path.split('.').reduce((acc, key) => (acc != null ? acc[key] : null), obj);
  }
}
