import styled from "@emotion/styled";

export const ModalRoot = styled.div`
  display: flex;
  flex-direction: column;
  min-height: 565px;
  max-height: 90vh;
`;

export const ModalBody = styled.div`
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  margin: 1rem 0 0;
  padding: 1rem 2rem 2rem;
  overflow-y: auto;
`;

export const ListRoot = styled.div`
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
`;
