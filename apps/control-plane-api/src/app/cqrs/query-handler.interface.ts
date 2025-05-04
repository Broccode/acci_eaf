/**
 * Query handler interface
 */
export interface QueryHandler<TQuery, TResult = any> {
  /**
   * Execute the query and return results
   * @param query The query to execute
   */
  execute(query: TQuery): Promise<TResult>;
} 