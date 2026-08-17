import { Chart, registerables } from 'chart.js';

// Register every chart.js v4 component (scales, elements, controllers, plugins)
// once so react-chartjs-2 charts work out of the box.
Chart.register(...registerables);

export { Chart };
