/* eslint-disable */
import { execSync } from 'child_process';

module.exports = async function () {
  // Stop services that were started in global-setup.
  console.log(globalThis.__TEARDOWN_MESSAGE__);
  
  // Clean up any running API processes we might have started
  // This assumes you're running on Linux/Mac where pkill is available
  try {
    console.log('Cleaning up API processes...');
    // Find and kill any node processes running our API
    // This is best-effort as the command might not exist on all systems
    execSync('pkill -f "nx serve control-plane-api" || true', { stdio: 'inherit' });
  } catch (error) {
    console.log('Warning: Could not clean up API processes automatically');
    console.log('You may need to manually stop the API service if it is still running');
  }
  
  console.log('E2E test teardown complete');
};
