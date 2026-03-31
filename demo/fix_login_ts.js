const fs = require('fs');
const path = 'D:\\sistemas\\miPyme_front\\miPyme\\src\\app\\components\\login\\login.component.ts';
let content = fs.readFileSync(path, 'utf8');

const target = `    this.errorMessage = '';
    this.isErrorVisible = false;

    if (!this.ssoCode || !this.username || !this.password) {`;

const replacement = `    this.errorMessage = '';
    this.isErrorVisible = false;
    this.submitted = true;

    if (!this.ssoCode || !this.username || !this.password) {`;

if (content.includes(target)) {
    content = content.replace(target, replacement);
    fs.writeFileSync(path, content);
    console.log('TS fixed');
} else {
    console.log('Target not found, checking if already fixed...');
    if (content.indexOf('this.submitted = true;\n\n    if (!this.ssoCode') !== -1) {
        console.log('Already fixed');
    } else {
        console.log('Could not find target block. Dumping relevant section for debug:');
        const start = content.indexOf('onLogin()');
        if (start !== -1) {
            console.log(content.substring(start, start + 400));
        }
    }
}
