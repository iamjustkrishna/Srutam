import {loadFont} from '@remotion/fonts';
import {Composition, type CalculateMetadataFunction, staticFile} from 'remotion';
import {Launch, type LaunchProps} from './Launch';
import {FPS} from './lib';
import {type Manifest, totalDur} from './timeline';

const FONTS: Array<[string, string, string, string]> = [
	['Inter', 'Inter-normal-400.woff2', '400', 'normal'], ['Inter', 'Inter-normal-500.woff2', '500', 'normal'],
	['Inter', 'Inter-normal-600.woff2', '600', 'normal'], ['Inter', 'Inter-normal-700.woff2', '700', 'normal'],
	['Inter', 'Inter-normal-800.woff2', '800', 'normal'],
	['Instrument Serif', 'InstrumentSerif-normal-400.woff2', '400', 'normal'], ['Instrument Serif', 'InstrumentSerif-italic-400.woff2', '400', 'italic'],
	['JetBrains Mono', 'JetBrainsMono-normal-400.woff2', '400', 'normal'], ['JetBrains Mono', 'JetBrainsMono-normal-600.woff2', '600', 'normal'],
];
FONTS.forEach(([family, f, weight, style]) => loadFont({family, url: staticFile('fonts/' + f), weight, style}));
loadFont({family: 'Roboto', url: staticFile('fonts/Roboto-var.woff2'), weight: '100 900'});
loadFont({family: 'Playfair Display', url: staticFile('fonts/playfair_display.ttf'), weight: '400 900'});

const calc: CalculateMetadataFunction<LaunchProps> = async ({props}) => {
	let manifest: Manifest | null = null;
	try {
		const r = await fetch(staticFile('vo/manifest.json'));
		if (r.ok) manifest = await r.json();
	} catch {
		manifest = null;
	}
	return {durationInFrames: Math.ceil(totalDur(manifest ?? undefined) * FPS), props: {...props, manifest}};
};

export const RemotionRoot = () => (
	<>
		<Composition id="Launch16x9" component={Launch} width={1920} height={1080} fps={FPS} durationInFrames={1800} defaultProps={{manifest: null}} calculateMetadata={calc} />
		<Composition id="Launch9x16" component={Launch} width={1080} height={1920} fps={FPS} durationInFrames={1800} defaultProps={{manifest: null}} calculateMetadata={calc} />
	</>
);
