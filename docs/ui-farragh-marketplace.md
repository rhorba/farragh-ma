# UI Foundation: Farragh.ma
**UX Reference**: docs/ux-farragh-marketplace.md
**Version**: 1.0 | **Date**: 2026-07-04 | **Author**: UI Designer

## 1. Design Approach
- **Strategy**: Angular Material (+ Angular CDK) as the component library
- **Rationale (YAGNI)**: Official Angular library, first-class RTL support via CDK `Directionality`/`dir` attribute — directly solves the FR/AR requirement without hand-rolling mirrored layouts. No need for a custom design system at this scope; theme it with tokens below instead of building from scratch.

## 2. Design Tokens
```css
/* Colors — recycling/green identity, kept to 2 brand + semantic set */
--color-primary:     #1B7F4D;   /* forest green — CTAs, active nav, links */
--color-primary-dark:#145C38;   /* hover/active states */
--color-secondary:   #2563EB;   /* blue — secondary actions (e.g., municipality zone tools) */
--color-background:  #FFFFFF;   /* dark mode: #0F0F0F */
--color-surface:      #F5F5F5;  /* dark mode: #1A1A1A */
--color-error:        #DC2626;
--color-success:      #16A34A;
--color-warning:      #F59E0B;
--color-text:         #111111;  /* dark mode: #F0F0F0 */
--color-text-muted:   #666666;  /* dark mode: #999999 */

/* Typography */
--font-family: 'Inter', 'Noto Sans Arabic', system-ui, sans-serif; /* Noto Sans Arabic covers AR glyphs cleanly */
--font-size-sm:  0.875rem;
--font-size-md:  1rem;
--font-size-lg:  1.25rem;
--font-size-xl:  1.5rem;

/* Spacing (4px base grid) */
--spacing-xs: 4px;  --spacing-sm: 8px;
--spacing-md: 16px; --spacing-lg: 24px;
--spacing-xl: 32px;
```
Dark mode: not required for MVP (not in PRD scope) — tokens above use CSS variables so it's a later toggle, not a rebuild, if ever requested.

## 3. Component Inventory
| Component | Reuse Existing (Angular Material) | Build New | Notes |
|---|---|---|---|
| Button (Primary/Secondary/Danger) | `mat-button`, `mat-raised-button` | No | Themed via tokens above |
| Form fields (text, select, textarea) | `mat-form-field` | No | Always-visible labels per UX rule |
| Status badge (Posted/Accepted/etc.) | — | Yes | Small custom chip component, color-coded per status |
| Request card (feed/list item) | `mat-card` as base | Yes (content layout) | Material + Recycler distance + CTA |
| Map/pin picker (address geo-point) | — | Yes | Thin wrapper around a map widget for address pin drop; PostGIS point on submit |
| Zone drawing tool (municipality/recycler) | — | Yes | Polygon/radius draw tool feeding `coverage_zones` |
| Bottom nav (mobile) | `mat-tab-nav-bar` or custom bottom bar | Yes (layout) | Per-role nav items from UX IA |
| Language switch (FR/AR) | — | Yes | Toggles Angular i18n locale + `dir` attribute |

## 4. Responsive Breakpoints
| Breakpoint | Width | Layout Notes |
|---|---|---|
| Mobile | < 768px | Single column, bottom nav, thumb-zone primary actions (matches UX rule) |
| Tablet | 768–1024px | Two-column for list+detail (feed/request detail side by side) |
| Desktop | > 1024px | Three-column possible for Admin (list/detail/actions), max content width 1280px |

## 5. RTL Strategy (hard requirement, not optional)
- Angular CDK `Directionality` service + `[dir]` binding on root, driven by `preferred_lang`/locale selection
- Angular Material components mirror automatically under `dir="rtl"` — icons/margins using logical CSS properties (`margin-inline-start`, not `margin-left`) everywhere in custom components (status badge, request card, zone tool, bottom nav)
- Every custom component in the inventory above must be manually verified in both `dir="ltr"` and `dir="rtl"` before merge (added to Test Strategy acceptance criteria)

## 6. Accessibility Baseline
- Color contrast: AA minimum (4.5:1 text, 3:1 large text/icons) — verified against tokens above (primary green on white passes AA)
- Focus indicators: Angular Material's default focus ring kept, not overridden
- Semantic HTML first; ARIA only where Material doesn't already provide it (e.g., custom status badge needs `role="status"` + `aria-label`)
- Touch targets ≥ 44x44px on mobile (custom components must match Material's button sizing)
