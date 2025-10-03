package booking

import (
	"context"
	"database/sql"
	"fmt"
)

// SQLTransactionManager implements TransactionManager for SQL databases
type SQLTransactionManager struct {
	db *sql.DB
}

// NewSQLTransactionManager creates a new SQL transaction manager
func NewSQLTransactionManager(db *sql.DB) *SQLTransactionManager {
	return &SQLTransactionManager{db: db}
}

// BeginTx starts a new database transaction
func (m *SQLTransactionManager) BeginTx(ctx context.Context) (Transaction, error) {
	tx, err := m.db.BeginTx(ctx, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to begin transaction: %w", err)
	}
	return &sqlTransaction{tx: tx}, nil
}

// sqlTransaction wraps sql.Tx to implement Transaction interface
type sqlTransaction struct {
	tx *sql.Tx
}

// Commit commits the transaction
func (t *sqlTransaction) Commit() error {
	return t.tx.Commit()
}

// Rollback rolls back the transaction
func (t *sqlTransaction) Rollback() error {
	return t.tx.Rollback()
}
