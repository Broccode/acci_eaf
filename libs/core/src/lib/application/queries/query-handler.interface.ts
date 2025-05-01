/**
 * Interface for query handlers
 */
export interface QueryHandler<TQuery, TResult> {
  /**
   * Execute the query
   * @param query The query to execute
   * @returns The query result
   */
  execute(query: TQuery): Promise<TResult>;
} 