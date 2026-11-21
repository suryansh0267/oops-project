# I-ITSM frontend

React 19 + Vite + Tailwind CSS 4. See the root README for how to run the whole system.

```bash
npm install
npm run dev      # http://localhost:5173, proxies /api to http://localhost:8080
npm run build    # production build in dist/
```

Structure:

- `src/components/ui.jsx`: design system (buttons, panels, badges, the live `SlaClock`, modal, table)
- `src/components/Layout.jsx`: role-aware shell and notification bell
- `src/pages/Home.jsx`: separate home screens for employees, technicians and admins
- `src/pages/admin/*`: administration screens
- `src/lib/`: formatting helpers and data hooks
